package com.github.oberdiah.deepcomplexity.indexes

import com.github.oberdiah.deepcomplexity.data.IndexValue
import com.github.oberdiah.deepcomplexity.services.MyProjectService
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.components.service
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.annotations.NonNls
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import java.io.DataInput
import java.io.DataOutput

val PRIMARY_INDEX_ID: @NonNls ID<String, IndexValue> = ID.create("DeepComplexity Primary Index")

class PrimaryIndex : FileBasedIndexExtension<String, IndexValue>() {
    private val indexer: PrimaryIndexer = PrimaryIndexer()

    override fun getName(): ID<String, IndexValue> {
        return PRIMARY_INDEX_ID
    }

    override fun getVersion(): Int {
        return 0
    }

    override fun dependsOnFileContent(): Boolean {
        return true
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    override fun getIndexer(): DataIndexer<String, IndexValue, FileContent> {
        return indexer
    }

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(
            JavaFileType.INSTANCE
        )
    }

    override fun getValueExternalizer(): DataExternalizer<IndexValue> {
        return object : DataExternalizer<IndexValue> {
            override fun save(out: DataOutput, value: IndexValue) {
                // Save the value
                out.writeUTF(value.data)
            }

            override fun read(input: DataInput): IndexValue {
                // Read the value
                return IndexValue(input.readUTF())
            }
        }
    }

    class PrimaryIndexer : DataIndexer<String, IndexValue, FileContent> {
        override fun map(inputData: FileContent): MutableMap<String, IndexValue> {
            inputData.psiFile.toUElement(UFile::class.java)?.let { uFile ->
                inputData.project.service<MyProjectService>().addToIndex(uFile)
            }

            return mutableMapOf()
        }
    }
}