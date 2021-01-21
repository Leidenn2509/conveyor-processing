package ru.nsu.fit.conveyor.commonNodes.img

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.delay
import ru.nsu.fit.conveyor.node.*

@ExperimentalCoroutinesApi
@Suppress("LeakingThis")
open class FilterNode(description: String, val filter: Filter) : NewBaseNode(description) {
    init {
        inputs[0] = NodeInput(this, 0, Image::class)
        outputs[0] = NodeOutput(this, 0, Image::class)
    }

    @Suppress("UNCHECKED_CAST")
    private val inputImg: Channel<Image>
        get() = inputs[0]!!.channel as Channel<Image>

    @Suppress("UNCHECKED_CAST")
    private val outputImg: Channel<Image>
        get() = outputs[0]!!.channel as Channel<Image>

    override suspend fun body() {
        log("Try receive image")
        val image = inputImg.receive()
        log("Receive image: $image")
        log("Do work")
        delay(DELAY)
        image.operations.add(filter)
        log("New image: $image")
        log("Send new image to outputs")
        outputImg.send(image)
        log("End of work")
    }

    companion object {
        const val DELAY = 1000L
    }
}

@ExperimentalCoroutinesApi
class GaussFilter(description: String = "Gauss Filter") : FilterNode(description, Gaussian())
