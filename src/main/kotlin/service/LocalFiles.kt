package service

import java.io.File

fun getAllFiles(directoryPath: String): List<File> {
    val directory = File(directoryPath)

    // Check if the directory exists and is actually a directory
    require(directory.exists() && directory.isDirectory) {
        "The provided path must be an existing directory"
    }

    // Use recursiveSequence to traverse all files in the directory and its subdirectories
    return directory.walkTopDown()
        .filter { it.isFile }
        .toList()
}