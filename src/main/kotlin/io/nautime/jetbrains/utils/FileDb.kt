package io.nautime.jetbrains.utils

import io.nautime.jetbrains.NauPlugin
import java.io.File
import java.io.FileWriter

class FileDb {
    private lateinit var dbFile: File
    private lateinit var writer: FileWriter
//    private lateinit var reader: FileReader




    fun init() {
        try {
            val userHome = getUserHomeDir()
            if (!userHome.exists()) {
                NauPlugin.log.warn("home directory doesn't exist. ${System.getProperty("user.home")}")
                return
            }

            dbFile = File(userHome, DB_FILE_NAME)
            if (!dbFile.exists()) {
                dbFile.createNewFile()
                NauPlugin.log.info("${dbFile.path} created")
            }

            writer = FileWriter(dbFile, true)
//            reader = FileReader(dbFile)

        } catch (ex: Exception) {
            NauPlugin.log.info("Error during FileWriter init", ex)
        }
    }

    fun add(lineStr: String) {
        writer.appendLine(lineStr)
        writer.flush()
    }

    fun removeAll() {
        writer.close()
        writer = FileWriter(dbFile)
    }

//    fun getAll(): List<String> {
//        return reader.readLines()
//    }

    fun close() {
        writer.close()
//        reader.close()
    }


    private fun getUserHomeDir(): File {
        return File(System.getProperty("user.home"))
    }


    companion object {
        const val DB_FILE_NAME = "nau.events.db"
    }
}
