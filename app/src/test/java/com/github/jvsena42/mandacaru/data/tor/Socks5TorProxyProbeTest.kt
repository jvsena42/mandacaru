package com.github.jvsena42.mandacaru.data.tor

import com.github.jvsena42.mandacaru.domain.settings.TorProxyAddress
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

class Socks5TorProxyProbeTest {

    private val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).apply {
        soTimeout = 5_000
    }
    private val proxy = TorProxyAddress("127.0.0.1", server.localPort)
    private val probe = Socks5TorProxyProbe(timeoutMs = 1_000)

    @After
    fun tearDown() = server.close()

    @Test
    fun `a SOCKS5 server accepting no-auth is reachable`() = runTest {
        var received = ByteArray(0)
        val worker = serveOnce { greeting, output ->
            received = greeting
            output.write(byteArrayOf(0x05, 0x00))
        }

        assertTrue(probe.isReachable(proxy))
        worker.join()
        assertArrayEquals(byteArrayOf(0x05, 0x01, 0x00), received)
    }

    @Test
    fun `a SOCKS5 server that wants authentication is not usable`() = runTest {
        val worker = serveOnce { _, output ->
            output.write(byteArrayOf(0x05, 0xFF.toByte()))
        }

        assertFalse(probe.isReachable(proxy))
        worker.join()
    }

    @Test
    fun `something that is not SOCKS5 is not usable`() = runTest {
        val worker = serveOnce { _, output -> output.write("HTTP/1.1 400\r\n".toByteArray()) }

        assertFalse(probe.isReachable(proxy))
        worker.join()
    }

    @Test
    fun `a listener that hangs up is not usable`() = runTest {
        val worker = serveOnce { _, _ -> }

        assertFalse(probe.isReachable(proxy))
        worker.join()
    }

    @Test
    fun `a closed port is not reachable`() = runTest {
        server.close()

        assertFalse(probe.isReachable(proxy))
    }

    /**
     * Reads the client's 3-byte greeting before handing over, so every test ends with a clean
     * close: unread input would turn the close into a reset instead of an EOF.
     */
    private fun serveOnce(handler: (ByteArray, OutputStream) -> Unit): Thread = thread {
        server.accept().use { client ->
            val greeting = client.getInputStream().readNBytes(3)
            handler(greeting, client.getOutputStream())
            client.getOutputStream().flush()
        }
    }
}
