//package ru.nsu.fit.conveyor.node
//
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.runBlocking
//import kotlinx.coroutines.withTimeout
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.Test
//import kotlin.math.pow
//
//class FlowTest {
//    @Test
//    fun testFlowCreation() {
//        val flow = Flow("id").apply {
//            addInput(0, Int::class.java)
//            addOutput(0, Int::class.java)
//            addNode("id", TestUtils.identityNode(Int::class.java))
//                .flowInput(0, 0)
//                .flowOutput(0, 0)
//        }
//    }
//
//    @Test
//    fun testTypeError() {
//        assertThrows(IllegalStateException::class.java, {
//            Flow("id").apply {
//                addInput(0, Int::class.java)
//                addOutput(0, Int::class.java)
//                addNode("id", TestUtils.identityNode(String::class.java))
//                    .flowInput(0, 0)
//                    .flowOutput(0, 0)
//            }
//        }, "Different channels' types")
//    }
//
//    @Test
//    fun testFlowRun() {
//        val flow = Flow("id").apply {
//            addInput(0, Int::class.java)
//            addOutput(0, Int::class.java)
//
//            addNode("id", TestUtils.identityNode(Int::class.java))
//                .flowInput(0, 0)
//                .flowOutput(0, 0)
//        }
//        runBlocking {
//            mapOf(0 to listOf(1)).let {
//                assertEquals(it, flow.run(it))
//            }
//
//            mapOf(0 to listOf(1, 2, 3)).let {
//                assertEquals(it, flow.run(it))
//            }
//        }
//    }
//
//    @Test
//    fun testPowFlow() {
//        val time = 1000L
//        val powNode = Node("simple pow").apply {
//            addInput(0, Double::class.java)
//            addOutput(0, Double::class.java)
//            body = { map ->
//                runBlocking {
//                    delay(time)
//                    @Suppress("UNCHECKED_CAST")
//                    val a = map.getValue(0) as List<Double>
//                    mapOf(0 to a.map { it * it })
//                }
//            }
//        }
//        val flow = Flow("x^8").apply {
//            addInput(0, Double::class.java)
//            addOutput(0, Double::class.java)
//
//            addNode("pow1", powNode).flowInput(0, 0)
//            addNode("pow2", powNode)
//            addNode("pow3", powNode).flowOutput(0, 0)
//
//            connect("pow1", 0, "pow2", 0)
//            connect("pow2", 0, "pow3", 0)
//        }
//        runBlocking {
//            mapOf(0 to listOf(1.0)).let {
//                assertEquals(it.map { (k, v) -> k to v.map { d -> d.pow(8) } }.toMap(), flow.run(it))
//            }
//
//            withTimeout(time * 6) {
//                mapOf(0 to listOf(1.0, 2.0, 3.0)).let {
//                    assertEquals(it.map { (k, v) -> k to v.map { d -> d.pow(8) } }.toMap(), flow.run(it))
//                }
//            }
//        }
//    }
//
//    @Test
//    fun testCyclicalFlow() {
//        val halfNode = Node("half").apply {
//            addInput(0, Double::class.java)
//
//            addOutput(0, Double::class.java)
//            addOutput(1, Double::class.java)
//            body = { map ->
//                val a = map.getValue(0).map { (it as Double) }
//                val res = a.mapNotNull { if (it > 1.0) it / 2 else null }
//                mapOf(
//                    0 to res,
//                    1 to res
//                )
//            }
//
//        }
//        val cyclicalFlow = Flow("cyclical").apply {
//            addInput(0, Double::class.java)
//            addOutput(0, Double::class.java)
//
//            addNode("half", halfNode)
//                .flowInput(0, 0)
//                .flowOutput(1, 0)
//            connect("half", 0, "half", 0)
//        }
//
//        runBlocking {
//            assertEquals(mapOf(0 to listOf(2.5, 1.25, 0.625)), cyclicalFlow.run(mapOf(0 to listOf(5.0))))
//        }
//    }
//
//    @Test
//    fun testComplexFlow() {
//        val powNode = Node("x^2").apply {
//            addInput(0, Double::class.java)
//            addOutput(0, Double::class.java)
//            body = { map ->
//                @Suppress("UNCHECKED_CAST")
//                val a = map.getValue(0) as List<Double>
//                mapOf(0 to a.map { it * it })
//            }
//        }
//        val flow = Flow("x^8").apply {
//            addInput(0, Double::class.java)
//            addOutput(0, Double::class.java)
//
//            addNode("pow1", powNode).flowInput(0, 0)
//            addNode("pow2", powNode)
//            addNode("pow3", powNode).flowOutput(0, 0)
//            connect("pow1", 0, "pow2", 0)
//            connect("pow2", 0, "pow3", 0)
//        }
//        val dividerNode = Node("x = 2 to 3").apply {
//            addInput(0, Double::class.java)
//            addOutput(0, Double::class.java)
//            addOutput(1, Double::class.java)
//            body = { map ->
//                val a = map.getValue(0).map { (it as Double) / 5.0 }
//                mapOf(
//                    0 to a.map { 2.0 * it },
//                    1 to a.map { 3.0 * it }
//                )
//            }
//        }
//
//        val subNode = Node("x-3").apply {
//            addInput(0, Double::class.java)
//            addOutput(0, Double::class.java)
//            body = { map ->
//                val a = map.getValue(0).map { (it as Double) - 3.0 }
//                mapOf(
//                    0 to a
//                )
//            }
//        }
//
//        val sumNode = Node("x+y").apply {
//            addInput(0, Double::class.java)
//            addInput(1, Double::class.java)
//            addOutput(0, Double::class.java)
//            body = { map ->
//                val a = map.getValue(0).map { it as Double }
//                val b = map.getValue(1).map { it as Double }
//                mapOf(
//                    0 to a.zip(b).map { it.first + it.second }
//                )
//            }
//        }
//
//        val complexFlow = Flow("hard math").apply {
//            addInput(0, Double::class.java)
//            addOutput(0, Double::class.java)
//
//            addNode("pow3", flow).flowInput(0, 0)
//
//            addNode("divider", dividerNode)
//            addNode("sub", subNode)
//            addNode("pow", powNode)
//            addNode("sum", sumNode).flowOutput(0, 0)
//
//            connect("pow3", 0, "divider", 0)
//
//            connect("divider", 0, "sub", 0)
//            connect("divider", 1, "pow", 0)
//            connect("sub", 0, "sum", 0)
//            connect("pow", 0, "sum", 1)
//        }
//        runBlocking {
//            mapOf(0 to listOf(5.0)).let {
//                assertEquals(it.map { (k, v) ->
//                    k to v.map { d ->
//                        d.pow(8).div(5).let {
//                            (2 * it - 3) + (3 * it).pow(2)
//                        }
//                    }
//                }.toMap(), complexFlow.run(it))
//            }
//
//            mapOf(0 to listOf(1.0, 2.0, 3.0)).let {
//                assertEquals(it.map { (k, v) ->
//                    k to v.map { d ->
//                        d.pow(8).div(5).let {
//                            (2 * it - 3) + (3 * it).pow(2)
//                        }
//                    }
//                }.toMap(), complexFlow.run(it))
//            }
//
//        }
//    }
//}