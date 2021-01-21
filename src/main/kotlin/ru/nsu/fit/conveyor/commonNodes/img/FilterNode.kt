package ru.nsu.fit.conveyor.commonNodes.img

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import ru.nsu.fit.conveyor.node.*

@ExperimentalCoroutinesApi
@Suppress("LeakingThis")
open class FilterNode(description: String, val filter: Filter) : BaseNode(description) {
    private inner class FilterContext: Context() {
        @Suppress("UNCHECKED_CAST")
        val inputImg: Channel<Image>
            get() = inputs[0]!!.channel as Channel<Image>

        @Suppress("UNCHECKED_CAST")
        val outputImg: Channel<Image>
            get() = outputs[0]!!.channel as Channel<Image>

        init {
            inputs[0] = NodeInput(this@FilterNode, 0, Image::class)
            outputs[0] = NodeOutput(this@FilterNode, 0, Image::class)
        }
    }

    override fun initContext(withContext: Any?) {
        contexts[withContext] = FilterContext()
    }

    init {
        initContext(null)
    }

    override suspend fun body(context: Context) = with(context as FilterContext) {
        log("Try receive image", context)
        val image = inputImg.receive()
        log("Receive image: $image", context)
        log("Do work", context)
        delay(DELAY)
        image.operations.add(filter)
        log("New image: $image", context)
        log("Send new image to outputs", context)
        outputImg.send(image)
        log("End of work", context)
    }

    companion object {
        const val DELAY = 1000L
    }
}

@ExperimentalCoroutinesApi
class GaussFilter(description: String = "Gauss Filter") : FilterNode(description, Gaussian())
