package org.nyanya.simplemusixbox

import java.io.File
import java.util.*

class MusicIterator(base: String) {
    var dir = File(base)
    var history: Stack<Pair<File, Int>> = Stack()
    var index = 0
    var files: Array<File> = arrayOf()

    init {

    }

    // GO DEPTH FIRST DIR ALWAYS FIRST

    fun dive(f: File) {
        history.push(Pair(dir, index+1))
        index = 0
        dir = f
        readDir()
        // keep diving if first item is Dir
    }

    private fun readDir() {
        files = dir.listFiles()
        Arrays.sort(files, FileComparator())
    }

    fun rise() {
        var prev = history.pop()
        index = prev.second
        dir = prev.first
        readDir()
        // keep rising if end of dir
    }

    fun nextTrack(): File? {
        // return null if no files found
        if (history.empty() && files.isEmpty()) { return null}
        // if run out of files in directory, rise
        while (index >= files.size) { rise() }
        // get next file
        var f = files[index]
        // while next file is a directory, dive
        while (f.isDirectory) {
            dive(f)
            if (index >= files.size) { rise() } // rise if subdir is empty
            f = files[index]
        }
        index++
        return f
    }
    fun nextRecord() {

    }

    inner class FileComparator : Comparator<File> {
        override fun compare(a: File, b: File): Int {
            return if (a.isDirectory && b.isFile) {
                -1
            } else if (b.isFile && a.isDirectory) {
                1
            } else {
                a.name.toLowerCase().compareTo(b.name.toLowerCase())
            }
        }
    }
}