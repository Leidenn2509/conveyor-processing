package ru.nsu.fit.conveyor.commonNodes.img

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import ru.nsu.fit.conveyor.node.BaseNode
import ru.nsu.fit.conveyor.node.NodeInput
import ru.nsu.fit.conveyor.node.NodeOutput

@ExperimentalCoroutinesApi
class SlicingNode : BaseNode("Slicing into areas") {
    private inner class SlicingNodeContext: Context() {
        init {
            inputs[0] = NodeInput(this@SlicingNode, 0, Image::class)
            inputs[1] = NodeInput(this@SlicingNode, 1, Int::class)
            outputs[0] = NodeOutput(this@SlicingNode, 0, Image::class)
        }

        @Suppress("UNCHECKED_CAST")
        val inputImage: Channel<Image>
            get() = inputs[0]!!.channel as Channel<Image>

        @Suppress("UNCHECKED_CAST")
        val inputN: Channel<Int>
            get() = inputs[1]!!.channel as Channel<Int>

        @Suppress("UNCHECKED_CAST")
        val output: Channel<Image>
            get() = outputs[0]!!.channel as Channel<Image>


        var n: Int? = null
    }

    override fun initContext(withContext: Any?) {
        contexts[withContext] = SlicingNodeContext()
    }

    init {
        initContext(null)
    }

    override fun isReady(withContext: Any?): Boolean = with(contexts[withContext] as SlicingNodeContext) {
        return if (n == null) {
            !inputN.isEmpty && !inputImage.isEmpty
        } else {
            !inputImage.isEmpty
        }
    }

    override suspend fun body(context: Context) = with(context as SlicingNodeContext) {
        log("slicing start", context)
        if (n == null) {
            n = inputN.receive()
        } else if (n != null && !inputN.isEmpty) {
            n = inputN.receive()
        }
        if (n!! <= 0) return

        val image = inputImage.receive()
        val width = image.width / n!!
        for (i in 0 until n!!) {
            delay(1000)
            val newImage = Image(
                width,
                image.height,
                image.type,
                mutableListOf(Slicing(i, n!!, image))
            )
            log("slicing send $newImage", context)
            output.send(
                newImage
            )
        }

    }
}