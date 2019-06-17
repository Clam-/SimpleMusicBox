package org.nyanya.simplemusixbox

import android.os.Environment
import android.os.Environment.*
import android.util.Log
import java.io.File
import java.io.FileFilter
import java.util.*

class MusicIterator() {
    var dir = File(".")
    var history: Stack<Pair<File, Int>> = Stack()
    var index = 0
    var files: Array<File> = arrayOf()
    var found: Boolean = false
    var online: Boolean = false

    init {

    }

    fun updateState() {
        Log.d("Musix", getExternalStorageState())
        online = ONLINE.contains(getExternalStorageState())
        if (online) {
            // TODO: Do permission check/request
            history.clear()
            dir = File(System.getenv("SECONDARY_STORAGE"))
            index = 0
            readDir()
            found = false
        }
    }

    fun dive(f: File, back: Boolean = false) {
        history.push(Pair(dir, index))
        dir = f
        readDir()
        // going backwards or not
        index = if (back && files.isNotEmpty()) files.size-1 else 0
        // skip empty dir
        if (index >= files.size) { rise(back) }
        // keep diving if first/current item is Dir
        if (files[index].isDirectory) { dive(files[index], back) }
    }

    // depth first dir first
    private fun readDir() {
        var ifiles: Array<File>? = dir.listFiles(MusicFilter())
        if (ifiles == null) {throw NoFiles() }
        else files = ifiles
        Arrays.sort(files, FileComparator())
    }

    fun rise(back: Boolean = false) {
        // reset if going backwards and at start of basedir
        if (back && (index == 0 && history.isEmpty())) {
            index = files.size-1
            if (!found) { throw NoFiles() }
            return
        }
        // reset if end of basedir
        if (index >= files.size && history.isEmpty()) {
            index = 0
            if (!found) { throw NoFiles() }
            return
        }
        var prev = history.pop()
        index = if (back) prev.second-1 else prev.second+1
        dir = prev.first
        readDir()
        // keep rising if end of dir
        if (index >= files.size) { rise(back) }
        // keep going backwards if less than size
        if (index < 0) { rise(back) }
        // dive if first/current item is Dir
        if (files[index].isDirectory) { dive(files[index], back) }
    }

    fun nextTrack(): File? {
        if (!online) { throw NoMedia() }
        // return null if no files found
        if (history.isEmpty() && files.isEmpty()) { return null }
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
        if (!online) { throw NoMedia() }
        when {
            index >= files.size -> rise()
            files[index].isDirectory -> dive(files[index])
            else -> rise()
        }
        return nextTrack()
    }

    fun prevTrack(): File? {
        if (!online) { throw NoMedia() }
        // return null if no files found
        if (history.isEmpty() && files.isEmpty()) { return null }
        // if run out of files in directory, rise
        if (index == 0) { rise(true) }
        // get next file
        var f = files[index]
        // while next file is a directory, dive
        if (f.isDirectory) { dive(f, true) }
        f = files[index]
        index++
        found = true
        return f
    }

    fun prevRecord(): File? {
        if (!online) { throw NoMedia() }
        if (history.isEmpty()) { index = 0 }
        else {
            rise()
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
    inner class NoMedia : Exception()

    private inner class MusicFilter : FileFilter {
        override fun accept(f: File?): Boolean {
            return when {
                f == null -> false
                f.isDirectory -> true
                else -> TYPES.contains(f.extension.toLowerCase())
            }
        }
    }

    companion object {
        private val TYPES = arrayOf("mp3", "flac", "m4a", "wav")
        private val ONLINE = arrayOf(MEDIA_MOUNTED, MEDIA_MOUNTED_READ_ONLY)
    }
}