package ru.nsu.fit.conveyor.commonNodes.img

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import ru.nsu.fit.conveyor.node.BaseNode
import ru.nsu.fit.conveyor.node.NodeInput
import ru.nsu.fit.conveyor.node.NodeOutput
import kotlin.random.Random

@ExperimentalCoroutinesApi
class ChooseNodeFromN(val n: Int) : BaseNode("ChooseNode") {
    private inner class ChooseNodeFromNContext : Context() {
        @Suppress("UNCHECKED_CAST")
        val outputImg: Channel<Image>
            get() = outputs[0]!!.channel as Channel<Image>

        init {
            outputs[0] = NodeOutput(this@ChooseNodeFromN, 0, Image::class)
            for (i in 0 until n) {
                inputs[i] = NodeInput(this@ChooseNodeFromN, i, Image::class)
            }
        }
    }

    override fun initContext(withContext: Any?) {
        contexts[withContext] = ChooseNodeFromNContext()
    }

    init {
        initContext(null)
    }

    override suspend fun body(context: Context) = with(context as ChooseNodeFromNContext) {
        val images = mutableListOf<Image>()
        inputs.forEach { (_, input) ->
            @Suppress("UNCHECKED_CAST")
            images.add((input.channel as Channel<Image>).receive())
        }
        delay(DELAY)
        outputImg.send(images[Random.nextInt(n)])
    }

    companion object {
        const val DELAY = 1500L
    }
}