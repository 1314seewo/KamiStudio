package com.hnl.kamistudio.util

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 二进制AndroidManifest.xml (AXML) 解析与修改器
 * 支持：添加Activity、移除LAUNCHER intent-filter、添加字符串
 */
object AXmlEditor {

    // AXML chunk types
    private const val CHUNK_STRING_POOL = 0x001C0001
    private const val CHUNK_RESOURCE_MAP = 0x00080180
    private const val CHUNK_START_NAMESPACE = 0x00100100
    private const val CHUNK_END_NAMESPACE = 0x00100101
    private const val CHUNK_START_ELEMENT = 0x00100102
    private const val CHUNK_END_ELEMENT = 0x00100103

    // 属性类型
    private const val TYPE_STRING = 0x03
    private const val TYPE_INT_BOOLEAN = 0x12
    private const val TYPE_INT_DEC = 0x10

    data class Attribute(
        var namespaceUri: Int = -1,
        var name: Int = -1,
        var rawValue: Int = -1,
        var size: Int = 8,
        var res0: Int = 0,
        var dataType: Int = 0,
        var data: Int = 0
    )

    data class XmlNode(
        var type: Int = 0,
        var lineNumber: Int = 0,
        var comment: Int = -1,
        var namespaceUri: Int = -1,
        var name: Int = -1,
        var prefix: Int = -1,
        var uri: Int = -1,
        var attributeStart: Int = 20,
        var attributeSize: Int = 20,
        var attributes: MutableList<Attribute> = mutableListOf(),
        var idIndex: Int = -1,
        var classIndex: Int = -1,
        var styleIndex: Int = -1,
        var children: MutableList<XmlNode> = mutableListOf()
    )

    data class AXmlDocument(
        var strings: MutableList<String> = mutableListOf(),
        var resourceIds: MutableList<Int> = mutableListOf(),
        var nodes: MutableList<XmlNode> = mutableListOf()
    )

    fun parse(bytes: ByteArray): AXmlDocument {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val doc = AXmlDocument()

        // 文件头
        val fileType = buf.int
        val fileSize = buf.int

        while (buf.position() < bytes.size) {
            val chunkStart = buf.position()
            val chunkType = buf.int
            val headerSize = buf.short.toInt()
            val chunkSize = buf.int

            when (chunkType) {
                CHUNK_STRING_POOL -> parseStringPool(buf, doc, chunkStart)
                CHUNK_RESOURCE_MAP -> parseResourceMap(buf, doc, chunkStart, chunkSize)
                CHUNK_START_NAMESPACE -> {
                    val node = parseStartNamespace(buf)
                    doc.nodes.add(node)
                }
                CHUNK_END_NAMESPACE -> {
                    val node = parseEndNamespace(buf)
                    doc.nodes.add(node)
                }
                CHUNK_START_ELEMENT -> {
                    val node = parseStartElement(buf)
                    doc.nodes.add(node)
                    // 递归解析子节点
                    parseChildren(buf, node)
                }
                CHUNK_END_ELEMENT -> {
                    // 不应该到这里，子节点在parseChildren中处理
                    buf.position(chunkStart + chunkSize)
                }
                else -> {
                    // 跳过未知chunk
                    buf.position(chunkStart + chunkSize)
                }
            }
        }

        return doc
    }

    private fun parseStringPool(buf: ByteBuffer, doc: AXmlDocument, chunkStart: Int) {
        val stringCount = buf.int
        val styleCount = buf.int
        val flags = buf.int
        val stringsStart = buf.int
        val stylesStart = buf.int

        val isUtf8 = (flags and 0x100) != 0

        // 字符串偏移
        val offsets = IntArray(stringCount)
        for (i in 0 until stringCount) {
            offsets[i] = buf.int
        }

        // 跳过样式偏移
        buf.position(buf.position() + styleCount * 4)

        // 字符串数据起始位置
        val stringDataStart = chunkStart + stringsStart

        for (i in 0 until stringCount) {
            buf.position(stringDataStart + offsets[i])
            val str = if (isUtf8) readUtf8String(buf) else readUtf16String(buf)
            doc.strings.add(str)
        }

        // 移动到chunk末尾
        val chunkSize = buf.getInt(chunkStart + 4)
        buf.position(chunkStart + chunkSize)
    }

    private fun readUtf8String(buf: ByteBuffer): String {
        // UTF-8字符串：字符长度(1或2字节) + 字节长度(1或2字节) + 数据 + 0x00
        var charLen = buf.get().toInt() and 0xFF
        if (charLen and 0x80 != 0) {
            charLen = ((charLen and 0x7F) shl 8) or (buf.get().toInt() and 0xFF)
        }
        var byteLen = buf.get().toInt() and 0xFF
        if (byteLen and 0x80 != 0) {
            byteLen = ((byteLen and 0x7F) shl 8) or (buf.get().toInt() and 0xFF)
        }
        val bytes = ByteArray(byteLen)
        buf.get(bytes)
        buf.get() // 0x00终止符
        return String(bytes, Charsets.UTF_8)
    }

    private fun readUtf16String(buf: ByteBuffer): String {
        // UTF-16LE字符串：长度(2或4字节) + 数据 + 0x0000
        var len = buf.short.toInt() and 0xFFFF
        if (len and 0x8000 != 0) {
            len = ((len and 0x7FFF) shl 16) or (buf.short.toInt() and 0xFFFF)
        }
        val bytes = ByteArray(len * 2)
        buf.get(bytes)
        buf.short // 0x0000终止符
        return String(bytes, Charsets.UTF_16LE)
    }

    private fun parseResourceMap(buf: ByteBuffer, doc: AXmlDocument, chunkStart: Int, chunkSize: Int) {
        val count = (chunkSize - 8) / 4
        for (i in 0 until count) {
            doc.resourceIds.add(buf.int)
        }
    }

    private fun parseStartNamespace(buf: ByteBuffer): XmlNode {
        val node = XmlNode(type = CHUNK_START_NAMESPACE)
        node.lineNumber = buf.int
        node.comment = buf.int
        node.prefix = buf.int
        node.uri = buf.int
        return node
    }

    private fun parseEndNamespace(buf: ByteBuffer): XmlNode {
        val node = XmlNode(type = CHUNK_END_NAMESPACE)
        node.lineNumber = buf.int
        node.comment = buf.int
        node.prefix = buf.int
        node.uri = buf.int
        return node
    }

    private fun parseStartElement(buf: ByteBuffer): XmlNode {
        val node = XmlNode(type = CHUNK_START_ELEMENT)
        node.lineNumber = buf.int
        node.comment = buf.int
        node.namespaceUri = buf.int
        node.name = buf.int
        node.attributeStart = buf.short.toInt() and 0xFFFF
        node.attributeSize = buf.short.toInt() and 0xFFFF
        val attrCount = buf.short.toInt() and 0xFFFF
        node.idIndex = buf.short.toInt() and 0xFFFF
        node.classIndex = buf.short.toInt() and 0xFFFF
        node.styleIndex = buf.short.toInt() and 0xFFFF

        for (i in 0 until attrCount) {
            val attr = Attribute()
            attr.namespaceUri = buf.int
            attr.name = buf.int
            attr.rawValue = buf.int
            attr.size = buf.short.toInt() and 0xFFFF
            attr.res0 = buf.get().toInt() and 0xFF
            attr.dataType = buf.get().toInt() and 0xFF
            attr.data = buf.int
            node.attributes.add(attr)
        }
        return node
    }

    private fun parseChildren(buf: ByteBuffer, parent: XmlNode) {
        while (true) {
            if (buf.position() >= buf.capacity()) break
            val chunkStart = buf.position()
            val chunkType = buf.int
            val headerSize = buf.short.toInt()
            val chunkSize = buf.int
            buf.position(chunkStart) // 重置

            when (chunkType) {
                CHUNK_START_ELEMENT -> {
                    val node = parseStartElement(buf)
                    parent.children.add(node)
                    parseChildren(buf, node)
                }
                CHUNK_END_ELEMENT -> {
                    // 消耗EndElement
                    buf.position(chunkStart + chunkSize)
                    return
                }
                CHUNK_START_NAMESPACE -> {
                    val node = parseStartNamespace(buf)
                    parent.children.add(node)
                }
                CHUNK_END_NAMESPACE -> {
                    val node = parseEndNamespace(buf)
                    parent.children.add(node)
                }
                else -> {
                    buf.position(chunkStart + chunkSize)
                }
            }
        }
    }

    fun addString(doc: AXmlDocument, s: String): Int {
        val idx = doc.strings.indexOf(s)
        if (idx >= 0) return idx
        doc.strings.add(s)
        return doc.strings.size - 1
    }

    fun getString(doc: AXmlDocument, index: Int): String {
        if (index < 0 || index >= doc.strings.size) return ""
        return doc.strings[index]
    }

    /**
     * 找到application节点
     */
    fun findApplication(doc: AXmlDocument): XmlNode? {
        fun findInNode(node: XmlNode): XmlNode? {
            if (node.type == CHUNK_START_ELEMENT && getString(doc, node.name) == "application") {
                return node
            }
            for (child in node.children) {
                findInNode(child)?.let { return it }
            }
            return null
        }
        for (node in doc.nodes) {
            findInNode(node)?.let { return it }
        }
        return null
    }

    /**
     * 找到启动Activity（有LAUNCHER intent-filter的）
     */
    fun findLauncherActivity(doc: AXmlDocument): XmlNode? {
        val app = findApplication(doc) ?: return null
        for (child in app.children) {
            if (child.type == CHUNK_START_ELEMENT && getString(doc, child.name) == "activity") {
                val hasLauncher = child.children.any { c ->
                    c.type == CHUNK_START_ELEMENT && getString(doc, c.name) == "intent-filter" &&
                        c.children.any { fc ->
                            fc.type == CHUNK_START_ELEMENT && getString(doc, fc.name) == "category" &&
                                fc.attributes.any { attr ->
                                    getString(doc, attr.name) == "name" && getString(doc, attr.data) == "android.intent.category.LAUNCHER"
                                }
                        }
                }
                if (hasLauncher) return child
            }
        }
        return null
    }

    /**
     * 移除Activity的LAUNCHER intent-filter
     */
    fun removeLauncherFilter(doc: AXmlDocument, activity: XmlNode) {
        val toRemove = mutableListOf<XmlNode>()
        for (child in activity.children) {
            if (child.type == CHUNK_START_ELEMENT && getString(doc, child.name) == "intent-filter") {
                val hasLauncher = child.children.any { fc ->
                    fc.type == CHUNK_START_ELEMENT && getString(doc, fc.name) == "category" &&
                        fc.attributes.any { attr ->
                            getString(doc, attr.name) == "name" && getString(doc, attr.data) == "android.intent.category.LAUNCHER"
                        }
                }
                if (hasLauncher) toRemove.add(child)
            }
        }
        activity.children.removeAll(toRemove)
    }

    /**
     * 添加卡密验证Activity到application
     */
    fun addVerifyActivity(doc: AXmlDocument, activityName: String, exported: Boolean = true) {
        val app = findApplication(doc) ?: return
        val androidNs = findAndroidNamespace(doc)

        val nameIdx = addString(doc, "name")
        val exportedIdx = addString(doc, "exported")
        val themeIdx = addString(doc, "theme")
        val activityNameIdx = addString(doc, activityName)
        val trueIdx = addString(doc, "true")
        val themeValIdx = addString(doc, "@android:style/Theme.Translucent.NoTitleBar")

        val activityNode = XmlNode(type = CHUNK_START_ELEMENT)
        activityNode.name = addString(doc, "activity")
        activityNode.namespaceUri = androidNs

        // name属性
        activityNode.attributes.add(Attribute(
            namespaceUri = androidNs,
            name = nameIdx,
            rawValue = activityNameIdx,
            dataType = TYPE_STRING,
            data = activityNameIdx
        ))
        // exported属性
        activityNode.attributes.add(Attribute(
            namespaceUri = androidNs,
            name = exportedIdx,
            rawValue = trueIdx,
            dataType = TYPE_INT_BOOLEAN,
            data = -1
        ))
        // theme属性
        activityNode.attributes.add(Attribute(
            namespaceUri = androidNs,
            name = themeIdx,
            rawValue = themeValIdx,
            dataType = TYPE_STRING,
            data = themeValIdx
        ))

        // intent-filter
        val actionIdx = addString(doc, "action")
        val categoryIdx = addString(doc, "category")
        val intentFilterIdx = addString(doc, "intent-filter")
        val mainActionIdx = addString(doc, "android.intent.action.MAIN")
        val launcherCategoryIdx = addString(doc, "android.intent.category.LAUNCHER")

        val intentFilter = XmlNode(type = CHUNK_START_ELEMENT, name = intentFilterIdx)
        val actionNode = XmlNode(type = CHUNK_START_ELEMENT, name = actionIdx, namespaceUri = androidNs)
        actionNode.attributes.add(Attribute(
            namespaceUri = androidNs, name = nameIdx, rawValue = mainActionIdx,
            dataType = TYPE_STRING, data = mainActionIdx
        ))
        val categoryNode = XmlNode(type = CHUNK_START_ELEMENT, name = categoryIdx, namespaceUri = androidNs)
        categoryNode.attributes.add(Attribute(
            namespaceUri = androidNs, name = nameIdx, rawValue = launcherCategoryIdx,
            dataType = TYPE_STRING, data = launcherCategoryIdx
        ))
        intentFilter.children.add(actionNode)
        intentFilter.children.add(categoryNode)

        activityNode.children.add(intentFilter)
        app.children.add(activityNode)
    }

    private fun findAndroidNamespace(doc: AXmlDocument): Int {
        for (node in doc.nodes) {
            if (node.type == CHUNK_START_NAMESPACE) {
                if (getString(doc, node.uri) == "http://schemas.android.com/apk/res/android") {
                    return node.uri
                }
            }
        }
        // 如果没找到，添加一个
        val uriIdx = addString(doc, "http://schemas.android.com/apk/res/android")
        val prefixIdx = addString(doc, "android")
        val ns = XmlNode(type = CHUNK_START_NAMESPACE, prefix = prefixIdx, uri = uriIdx)
        doc.nodes.add(0, ns)
        return uriIdx
    }

    /**
     * 序列化为二进制AXML
     */
    fun serialize(doc: AXmlDocument): ByteArray {
        // 先序列化节点，计算大小
        val nodeBytes = serializeNodes(doc)
        val stringPoolBytes = serializeStringPool(doc.strings)
        val resourceMapBytes = serializeResourceMap(doc.resourceIds)

        val totalSize = 8 + stringPoolBytes.size + resourceMapBytes.size + nodeBytes.size

        val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        // 文件头
        buf.putInt(0x00080003)
        buf.putInt(totalSize)
        // StringPool
        buf.put(stringPoolBytes)
        // ResourceMap
        buf.put(resourceMapBytes)
        // Nodes
        buf.put(nodeBytes)

        return buf.array()
    }

    private fun serializeStringPool(strings: List<String>): ByteArray {
        // 计算字符串数据大小
        val stringData = mutableListOf<ByteArray>()
        var dataSize = 0
        for (s in strings) {
            val bytes = s.toByteArray(Charsets.UTF_8)
            // UTF-8: charLen(1-2) + byteLen(1-2) + data + 0x00
            val charLen = s.length
            val byteLen = bytes.size
            val header = mutableListOf<Byte>()
            if (charLen > 0x7F) {
                header.add(((charLen shr 8) or 0x80).toByte())
                header.add((charLen and 0xFF).toByte())
            } else {
                header.add(charLen.toByte())
            }
            if (byteLen > 0x7F) {
                header.add(((byteLen shr 8) or 0x80).toByte())
                header.add((byteLen and 0xFF).toByte())
            } else {
                header.add(byteLen.toByte())
            }
            val entry = header.toByteArray() + bytes + byteArrayOf(0)
            stringData.add(entry)
            dataSize += entry.size
        }

        val headerSize = 28
        val offsetsSize = strings.size * 4
        val stringsStart = headerSize + offsetsSize
        val chunkSize = stringsStart + dataSize

        val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(CHUNK_STRING_POOL)
        buf.putShort(28) // headerSize
        buf.putShort(0) // 保留
        buf.putInt(chunkSize)
        buf.putInt(strings.size)
        buf.putInt(0) // styleCount
        buf.putInt(0x100) // flags: UTF-8
        buf.putInt(stringsStart)
        buf.putInt(0) // stylesStart

        // 偏移
        var offset = 0
        for (data in stringData) {
            buf.putInt(offset)
            offset += data.size
        }

        // 字符串数据
        for (data in stringData) {
            buf.put(data)
        }

        return buf.array()
    }

    private fun serializeResourceMap(ids: List<Int>): ByteArray {
        if (ids.isEmpty()) return ByteArray(0)
        val size = 8 + ids.size * 4
        val buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(CHUNK_RESOURCE_MAP)
        buf.putShort(8)
        buf.putShort(0)
        buf.putInt(size)
        for (id in ids) buf.putInt(id)
        return buf.array()
    }

    private fun serializeNodes(doc: AXmlDocument): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        for (node in doc.nodes) {
            serializeNode(node, out)
        }
        return out.toByteArray()
    }

    private fun serializeNode(node: XmlNode, out: java.io.ByteArrayOutputStream) {
        val buf = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)

        when (node.type) {
            CHUNK_START_NAMESPACE -> {
                buf.putInt(CHUNK_START_NAMESPACE)
                buf.putShort(16)
                buf.putShort(0)
                buf.putInt(16) // chunkSize
                buf.putInt(node.lineNumber)
                buf.putInt(node.comment)
                buf.putInt(node.prefix)
                buf.putInt(node.uri)
            }
            CHUNK_END_NAMESPACE -> {
                buf.putInt(CHUNK_END_NAMESPACE)
                buf.putShort(16)
                buf.putShort(0)
                buf.putInt(16)
                buf.putInt(node.lineNumber)
                buf.putInt(node.comment)
                buf.putInt(node.prefix)
                buf.putInt(node.uri)
            }
            CHUNK_START_ELEMENT -> {
                val attrCount = node.attributes.size
                val nodeSize = 16 + 8 + attrCount * 20 // header + ext + attributes
                buf.putInt(CHUNK_START_ELEMENT)
                buf.putShort(16)
                buf.putShort(0)
                buf.putInt(nodeSize)
                buf.putInt(node.lineNumber)
                buf.putInt(node.comment)
                buf.putInt(node.namespaceUri)
                buf.putInt(node.name)
                buf.putShort(20.toShort()) // attributeStart
                buf.putShort(20.toShort()) // attributeSize
                buf.putShort(attrCount.toShort())
                buf.putShort(0) // idIndex
                buf.putShort(0) // classIndex
                buf.putShort(0) // styleIndex

                for (attr in node.attributes) {
                    buf.putInt(attr.namespaceUri)
                    buf.putInt(attr.name)
                    buf.putInt(attr.rawValue)
                    buf.putShort(attr.size.toShort())
                    buf.put(attr.res0.toByte())
                    buf.put(attr.dataType.toByte())
                    buf.putInt(attr.data)
                }

                out.write(buf.array(), 0, buf.position())

                // 子节点
                for (child in node.children) {
                    serializeNode(child, out)
                }

                // EndElement
                val endBuf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
                endBuf.putInt(CHUNK_END_ELEMENT)
                endBuf.putShort(16)
                endBuf.putShort(0)
                endBuf.putInt(16)
                endBuf.putInt(node.lineNumber)
                endBuf.putInt(node.comment)
                endBuf.putInt(node.namespaceUri)
                endBuf.putInt(node.name)
                out.write(endBuf.array())
                return
            }
        }

        out.write(buf.array(), 0, buf.position())
    }
}
