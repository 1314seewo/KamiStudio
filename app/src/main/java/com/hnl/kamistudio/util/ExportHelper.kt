package com.hnl.kamistudio.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.hnl.kamistudio.data.KamiEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 导出工具类
 * Android 10+ 使用 MediaStore 写入公共Downloads目录，无需存储权限
 * Android 9及以下 使用传统File方式
 */
object ExportHelper {

    private const val FOLDER_NAME = "KamiStudio"

    /**
     * 保存文件到公共Downloads/KamiStudio目录
     * 返回文件的content:// URI（用于分享）和显示名
     */
    private data class SavedFile(val uri: Uri, val displayName: String, val filePath: String)

    private fun saveToDownloads(context: Context, fileName: String, mimeType: String, data: ByteArray): SavedFile? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用MediaStore
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
                SavedFile(uri, fileName, "Download/$FOLDER_NAME/$fileName")
            } else {
                // Android 9及以下 使用传统File
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER_NAME)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { it.write(data) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                SavedFile(uri, fileName, "Download/$FOLDER_NAME/$fileName")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 最后兜底：保存到APP私有目录
            try {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), FOLDER_NAME)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { it.write(data) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                SavedFile(uri, fileName, "Android/data/${context.packageName}/files/Download/$FOLDER_NAME/$fileName")
            } catch (e2: Exception) {
                e2.printStackTrace()
                null
            }
        }
    }

    fun exportToTxt(context: Context, kamiList: List<KamiEntity>, fileName: String = "卡密列表"): SavedFile? {
        val sb = StringBuilder()
        sb.append("===== 卡密导出 - ${kamiList.size} 张 =====\n")
        sb.append("导出时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}\n")
        sb.append("========================================\n\n")
        kamiList.forEachIndexed { index, kami ->
            sb.append("${index + 1}. ${kami.code}\n")
            sb.append("   类型: ${KamiGenerator.getTypeLabel(kami.type)} | 状态: ${getStatusLabel(kami.status)}\n")
            if (kami.note.isNotEmpty()) sb.append("   备注: ${kami.note}\n")
            sb.append("\n")
        }
        return saveToDownloads(context, "$fileName.txt", "text/plain", sb.toString().toByteArray())
    }

    fun exportToCsv(context: Context, kamiList: List<KamiEntity>, fileName: String = "卡密列表"): SavedFile? {
        val sb = StringBuilder()
        sb.append("卡密,类型,有效期(天),状态,生成时间,激活时间,备注\n")
        kamiList.forEach { kami ->
            sb.append("${kami.code},${KamiGenerator.getTypeLabel(kami.type)},${kami.validDays},${getStatusLabel(kami.status)},${kami.createdAt},${kami.activatedAt ?: ""},${kami.note}\n")
        }
        return saveToDownloads(context, "$fileName.csv", "text/csv", sb.toString().toByteArray())
    }

    /**
     * 把卡密注入相关的4个文件打包成一个zip，保存到Downloads
     */
    fun exportInjectPackage(
        context: Context,
        smali: String,
        manifest: String,
        guide: String,
        proguard: String,
        packageName: String = "卡密注入包"
    ): SavedFile? {
        return try {
            // 在cacheDir临时打包
            val tempDir = File(context.cacheDir, "inject_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            File(tempDir, "1_卡密验证Activity.smali").writeText(smali)
            File(tempDir, "2_AndroidManifest配置.xml").writeText(manifest)
            File(tempDir, "3_注入操作指南.txt").writeText(guide)
            File(tempDir, "4_混淆规则.pro").writeText(proguard)

            val tempZip = File(tempDir, "$packageName.zip")
            java.util.zip.ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
                tempDir.listFiles()?.filter { it.name.endsWith(".smali") || it.name.endsWith(".xml") || it.name.endsWith(".txt") || it.name.endsWith(".pro") }?.forEach { file ->
                    FileInputStream(file).use { fis ->
                        val entry = java.util.zip.ZipEntry(file.name)
                        zos.putNextEntry(entry)
                        fis.copyTo(zos)
                        zos.closeEntry()
                    }
                }
            }

            val data = tempZip.readBytes()
            tempDir.deleteRecursively()

            saveToDownloads(context, "$packageName.zip", "application/zip", data)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 分享已保存的文件
     */
    fun shareSavedFile(context: Context, savedFile: SavedFile) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when {
                    savedFile.displayName.endsWith(".zip") -> "application/zip"
                    savedFile.displayName.endsWith(".csv") -> "text/csv"
                    else -> "text/plain"
                }
                putExtra(Intent.EXTRA_STREAM, savedFile.uri)
                putExtra(Intent.EXTRA_SUBJECT, savedFile.displayName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "保存/分享文件"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 兼容旧接口：返回File（用于还没改的地方）
    fun exportToTxtFile(context: Context, kamiList: List<KamiEntity>, fileName: String = "卡密列表"): File? {
        val saved = exportToTxt(context, kamiList, fileName)
        return saved?.let { File(it.filePath) }
    }

    fun exportToCsvFile(context: Context, kamiList: List<KamiEntity>, fileName: String = "卡密列表"): File? {
        val saved = exportToCsv(context, kamiList, fileName)
        return saved?.let { File(it.filePath) }
    }

    private fun getStatusLabel(status: String): String = when (status) {
        "inactive" -> "未激活"
        "active" -> "已激活"
        "expired" -> "已过期"
        "disabled" -> "已禁用"
        else -> status
    }
}
