package com.hnl.kamistudio.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.hnl.kamistudio.data.KamiEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExportHelper {

    // 统一保存到公共Downloads/KamiStudio目录，用户容易找到
    private fun getExportDir(context: Context): File {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KamiStudio")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun exportToTxt(context: Context, kamiList: List<KamiEntity>, fileName: String = "卡密列表"): File? {
        return try {
            val file = File(getExportDir(context), "$fileName.txt")
            FileWriter(file).use { writer ->
                writer.write("===== 卡密导出 - ${kamiList.size} 张 =====\n")
                writer.write("导出时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}\n")
                writer.write("========================================\n\n")
                kamiList.forEachIndexed { index, kami ->
                    writer.write("${index + 1}. ${kami.code}\n")
                    writer.write("   类型: ${KamiGenerator.getTypeLabel(kami.type)} | 状态: ${getStatusLabel(kami.status)}\n")
                    if (kami.note.isNotEmpty()) writer.write("   备注: ${kami.note}\n")
                    writer.write("\n")
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            // fallback到app私有目录
            try {
                val dir = File(context.getExternalFilesDir(null), "KamiStudio")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "$fileName.txt")
                file.writeText("导出失败，这是备用文件")
                file
            } catch (e2: Exception) { null }
        }
    }

    fun exportToCsv(context: Context, kamiList: List<KamiEntity>, fileName: String = "卡密列表"): File? {
        return try {
            val file = File(getExportDir(context), "$fileName.csv")
            FileWriter(file).use { writer ->
                writer.write("卡密,类型,有效期(天),状态,生成时间,激活时间,备注\n")
                kamiList.forEach { kami ->
                    writer.write("${kami.code},${KamiGenerator.getTypeLabel(kami.type)},${kami.validDays},${getStatusLabel(kami.status)},${kami.createdAt},${kami.activatedAt ?: ""},${kami.note}\n")
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 把卡密注入相关的4个文件打包成一个zip，方便用户使用
     */
    fun exportInjectPackage(
        context: Context,
        smali: String,
        manifest: String,
        guide: String,
        proguard: String,
        packageName: String = "卡密注入包"
    ): File? {
        return try {
            val dir = File(context.cacheDir, "inject_temp")
            if (!dir.exists()) dir.mkdirs()

            File(dir, "1_卡密验证Activity.smali").writeText(smali)
            File(dir, "2_AndroidManifest配置.xml").writeText(manifest)
            File(dir, "3_注入操作指南.txt").writeText(guide)
            File(dir, "4_混淆规则.pro").writeText(proguard)

            // 打包成zip
            val zipFile = File(getExportDir(context), "$packageName.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                dir.listFiles()?.forEach { file ->
                    FileInputStream(file).use { fis ->
                        val entry = ZipEntry(file.name)
                        zos.putNextEntry(entry)
                        fis.copyTo(zos)
                        zos.closeEntry()
                    }
                }
            }

            // 清理临时文件
            dir.deleteRecursively()
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareFile(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when {
                    file.name.endsWith(".zip") -> "application/zip"
                    file.name.endsWith(".csv") -> "text/csv"
                    else -> "text/plain"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "保存/分享文件"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFileDisplayName(file: File): String {
        return file.name
    }

    fun getFileSimplePath(file: File): String {
        return "Download/KamiStudio/${file.name}"
    }

    private fun getStatusLabel(status: String): String = when (status) {
        "inactive" -> "未激活"
        "active" -> "已激活"
        "expired" -> "已过期"
        "disabled" -> "已禁用"
        else -> status
    }
}
