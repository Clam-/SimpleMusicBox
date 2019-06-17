package org.nyanya.simplemusixbox

import java.io.File
import java.io.FileFilter
import java.util.*

class MusicIterator(base: String) {
    var dir = File(base)
    var history: Stack<Pair<File, Int>> = Stack()
    var index = 0
    var files: Array<File> = arrayOf()
    var found: Boolean = false

    init {
        
    }

    fun dive(f: File) {
        history.push(Pair(dir, index+1))
        index = 0
        dir = f
        readDir()
        // skip empty dir
        if (index >= files.size) { rise() }
        // keep diving if first/current item is Dir
        if (files[index].isDirectory) { dive(files[index]) }
    }

    // depth first dir first
    private fun readDir() {
        files = dir.listFiles(MusicFilter())
        Arrays.sort(files, FileComparator())
    }

    fun rise() {
        // reset if end of basedir
        if (index >= files.size && history.empty()) {
            index = 0
            if (!found) { throw NoFiles() }
            return
        }
        var prev = history.pop()
        index = prev.second
        dir = prev.first
        readDir()
        // keep rising if end of dir
        if (index >= files.size) { rise() }
        // dive if first/current item is Dir
        if (files[index].isDirectory) { dive(files[index]) }
    }

    fun nextTrack(): File? {
        // return null if no files found
        if (history.empty() && files.isEmpty()) { return null }
        // if run out of files in directory, rise
        if (index >= files.size) { rise() }
        // get next file
        var f = files[index]
        // while next file is a directory, dive
        if (f.isDirectory) { dive(f) }
        f = files[index]
        index++
        found = true
        return f
    }
    fun nextRecord() : File? {
        when {
            index >= files.size -> rise()
            files[index].isDirectory -> dive(files[index])
            else -> rise()
        }
        return nextTrack()
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

    inner class NoFiles : Exception()

    private inner class MusicFilter : FileFilter {
        override fun accept(f: File?): Boolean {
            return when {
                f == null -> false
                f.isDirectory -> true
                else -> TYPES.contains(f.extension)
            }
        }
    }

    companion object {
        private val TYPES = arrayOf("mp3", "flac", "m4a", "wav")
    }
}