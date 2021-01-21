//package ru.nsu.fit.conveyor.node
//
//import kotlinx.coroutines.runBlocking
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.Test
//
//class NodeTest {
//
//    @Test
//    fun testNodeCreation() {
//        val node = Node("node")
//    }
//
//    @Test
//    fun testRunSimpleNode() {
//        val identityNode = TestUtils.identityNode()
//        runBlocking {
//            mapOf(0 to listOf<Any>(1)).let {
//                assertEquals(identityNode.run(it), it)
//            }
//
//            mapOf(0 to listOf<Any>(1, 2, 3)).let {
//                assertEquals(identityNode.run(it), it)
//            }
//        }
//    }
//}