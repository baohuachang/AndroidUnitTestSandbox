package com.natural.androidunittestsandbox

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.lang.RuntimeException

class ExampleMockTest {
    @Test
    fun `Depended On Component`() {
        val doc1: Dependency1 = mockk()
        val doc2: Dependency2 = mockk()

        every { doc1.value1 } returns 5
        every { doc2.value2 } returns "6"

        val sut = SystemUnderTest(doc1, doc2)

        assertEquals(11, sut.calculate())
    }

    @Test
    fun `Argument Matching`() {
        val mock: Mock = mockk()
        every { mock.call(more(5)) } returns 1
        every { mock.call(or(less(5), eq(5))) } returns -1
    }

    @Test
    fun `Behavior Verification`() {
        var mock: Mock = mockk()
        every { mock.call() } just Runs

        verify {
            mock wasNot Called
        }

        mock.call()

        verify {
            mock.call()
        }

        every { mock.call(1) } returns 1
        mock.call(1)
        mock.call(1)
        mock.call(1)
        verify(atLeast = 3, atMost = 7) {
            mock.call(1)
        }
        verify(exactly = 3) { mock.call(1)  }

        verify(exactly = 0) {
            mock.call(1,1)
            mock.call(2)
            mock.call(less(1))
        }

        mock = mockk()
        every { mock.call(any()) } returns 1
        mock.call(1)
        mock.call(2)
        mock.call(3)
        verifySequence {
            mock.call(1)
            mock.call(2)
            mock.call(3)
        }
        verifyOrder {
            mock.call(1)
            mock.call(3)
        }

    }

    @Test
    fun `Expected Answer`() {
        val mock: Mock = mockk()

        every { mock.call(1) } returns 1
        assertEquals(1, mock.call(1))

        every { mock.call(1) } returnsMany listOf(2,3)
        assertEquals(2, mock.call(1))
        assertEquals(3, mock.call(1))

        every { mock.call(1) } returns 4 andThen 5 andThen 6
        assertEquals(4, mock.call(1))
        assertEquals(5, mock.call(1))
        assertEquals(6, mock.call(1))

        every { mock.call(1) } throws RuntimeException("error happened")
        assertThrows(RuntimeException::class.java) { mock.call(1) }

        every { mock.call() } just Runs
        mock.call()

        every { mock.call(1) } answers { 1 + 1}
        assertEquals(2, mock.call(1))
    }

    @Test
    fun `Capturing`() {
        val slot = slot<Int>()
        val mock: Mock = mockk()

        every { mock.call(capture(slot), any()) } answers { slot.captured * 10 }

        var result = mock.call(2,2)

        assertEquals(2, slot.captured)
        assertEquals(20, result)

        val list = mutableListOf<Int>()

        every { mock.call(capture(list), capture(list))} answers { list[0] + list[1] }

        result = mock.call(2,3)


        assertEquals(2, list[0])
        assertEquals(3, list[1])
        assertEquals(5, result)
    }

    @Test
    fun `Relaxed Mocks`() {
        val mock: Mock = mockk(relaxed = true)

        val result = mock.call(1)

        assertEquals(0, result)

        verify {
            mock.call(1)
        }

        // without this, mock.chain().chain() will erase last call
        // that cause chain() only be called once.
        // Note however that due to natural language limits it is not possible
        // to have chains of calls work for generic return types as this information
        // got erased.
        every { mock.chain() } returns mock
        mock.chain().chain()

        verify(exactly = 2) {
            mock.chain()
        }
    }

    @Test
    fun `Spies`() {
        var spy = spyk(Adder())
        assertEquals(9, spy.add(4, 5))

        every { spy.magnify(any()) } answers {
            this.firstArg<Int>() * 2
        }
        assertEquals(14, spy.add(4, 5))

        verify {
            spy.add(4, 5)
            spy.magnify(5)
        }
    }
}

class MockAnnotationTest {

    @MockK
    lateinit var doc1: Dependency1
    @RelaxedMockK
    lateinit var doc2: Dependency2
    //@SpyK
    val doc3 = spyk(Dependency2("1"))

    @Before
    fun setUp() = MockKAnnotations.init(this)

    @Test
    fun `Annotation works correct`() {
        every { doc1.value1 } returns 1
        every { doc2.value2 } returns "0"
        val sut = SystemUnderTest(doc1, doc2)
        val result = sut.calculate()
        assertEquals(1, result)
        verify {
            doc1.value1
            doc2.value2
        }

    }
}

class Adder {
    fun magnify(a: Int) = a
    fun add(a: Int, b: Int) = a + magnify(b)
}

interface Mock {
    fun call(): Unit
    fun call(arg1: Int) : Int
    fun call(arg1: Int, arg2: Int): Int
    fun chain(): Mock
}

class Dependency1(val value1: Int)
class Dependency2(val value2: String)
class SystemUnderTest (
    val dependency1: Dependency1,
    val dependency2: Dependency2
) {
    fun calculate() =
        dependency1.value1 + dependency2.value2.toInt()
}