package com.hnl.kamistudio.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.hnl.kamistudio.data.KamiEntity
import java.io.File
import java.io.FileWriter

object ExportHelper {

    fun exportToTxt(context: Context, kamiList: List<KamiEntity>, fileName: String = "kami_codes"): File? {
        return try {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "KamiStudio")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "$fileName.txt")
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
            null
        }
    }

    fun exportToCsv(context: Context, kamiList: List<KamiEntity>, fileName: String = "kami_codes"): File? {
        return try {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "KamiStudio")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "$fileName.csv")
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

    fun exportSmaliCode(context: Context, smali: String, manifest: String, guide: String, proguard: String): File? {
        return try {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "KamiStudio/inject")
            if (!dir.exists()) dir.mkdirs()
            File(dir, "KamiVerifyActivity.smali").writeText(smali)
            File(dir, "AndroidManifest_config.xml").writeText(manifest)
            File(dir, "注入指南.txt").writeText(guide)
            File(dir, "proguard-rules.pro").writeText(proguard)
            dir
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享文件"))
    }

    private fun getStatusLabel(status: String): String = when (status) {
        "inactive" -> "未激活"
        "active" -> "已激活"
        "expired" -> "已过期"
        "disabled" -> "已禁用"
        else -> status
    }
}
