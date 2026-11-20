package com.github.jvsena42.mandacaru.data.tor

import com.github.jvsena42.mandacaru.domain.settings.TorProxyAddress
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
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
        var greeting = ByteArray(0)
        val worker = serveOnce { input, output ->
            greeting = input.readNBytes(3)
            output.write(byteArrayOf(0x05, 0x00))
        }

        assertTrue(probe.isReachable(proxy))
        worker.join()
        assertArrayEquals(byteArrayOf(0x05, 0x01, 0x00), greeting)
    }

    @Test
    fun `a SOCKS5 server that wants authentication is not usable`() = runTest {
        val worker = serveOnce { input, output ->
            input.readNBytes(3)
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

    private fun serveOnce(
        handler: (java.io.InputStream, java.io.OutputStream) -> Unit,
    ): Thread = thread {
        // Drains the greeting before closing: unread input would turn the close into a reset.
        server.accept().use { client ->
            handler(client.getInputStream(), client.getOutputStream())
            client.getOutputStream().flush()
        }
    }
}
