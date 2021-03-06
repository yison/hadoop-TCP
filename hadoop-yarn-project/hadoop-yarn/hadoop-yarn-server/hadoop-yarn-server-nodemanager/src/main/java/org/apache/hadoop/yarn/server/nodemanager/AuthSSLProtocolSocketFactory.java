/*
 * $HeadURL$
 * $Revision$
 * $Date$
 *
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

//package org.ovirt.engine.core.utils.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.http.conn.ssl.*;

/**
 * <p>
 * AuthSSLProtocolSocketFactory can be used to validate the identity of the HTTPS server against a list of trusted
 * certificates and to authenticate to the HTTPS server using a private key.
 * </p>
 *
 * <p>
 * AuthSSLProtocolSocketFactory will enable server authentication when supplied with a {@link KeyStore truststore} file
 * containg one or several trusted certificates. The client secure socket will reject the connection during the SSL
 * session handshake if the target HTTPS server attempts to authenticate itself with a non-trusted certificate.
 * </p>
 *
 * <p>
 * Use JDK keytool utility to import a trusted certificate and generate a truststore file:
 *
 * <pre>
 *     keytool -import -alias "my server cert" -file server.crt -keystore my.truststore
 * </pre>
 *
 * </p>
 *
 * <p>
 * AuthSSLProtocolSocketFactory will enable client authentication when supplied with a {@link KeyStore keystore} file
 * containg a private key/public certificate pair. The client secure socket will use the private key to authenticate
 * itself to the target HTTPS server during the SSL session handshake if requested to do so by the server. The target
 * HTTPS server will in its turn verify the certificate presented by the client in order to establish client's
 * authenticity
 * </p>
 *
 * <p>
 * Use the following sequence of actions to generate a keystore file
 * </p>
 * <ul>
 * <li>
 * <p>
 * Use JDK keytool utility to generate a new key
 *
 * <pre>
 * keytool -genkey -v -alias "my client key" -validity 365 -keystore my.keystore
 * </pre>
 *
 * For simplicity use the same password for the key as that of the keystore
 * </p>
 * </li>
 * <li>
 * <p>
 * Issue a certificate signing request (CSR)
 *
 * <pre>
 * keytool -certreq -alias "my client key" -file mycertreq.csr -keystore my.keystore
 * </pre>
 *
 * </p>
 * </li>
 * <li>
 * <p>
 * Send the certificate request to the trusted Certificate Authority for signature. One may choose to act as her own CA
 * and sign the certificate request using a PKI tool, such as OpenSSL.
 * </p>
 * </li>
 * <li>
 * <p>
 * Import the trusted CA root certificate
 *
 * <pre>
 * keytool -import -alias "my trusted ca" -file caroot.crt -keystore my.keystore
 * </pre>
 *
 * </p>
 * </li>
 * <li>
 * <p>
 * Import the PKCS#7 file containg the complete certificate chain
 *
 * <pre>
 * keytool -import -alias "my client key" -file mycert.p7 -keystore my.keystore
 * </pre>
 *
 * </p>
 * </li>
 * <li>
 * <p>
 * Verify the content the resultant keystore file
 *
 * <pre>
 * keytool -list -v -keystore my.keystore
 * </pre>
 *
 * </p>
 * </li>
 * </ul>
 * <p>
 * Example of using custom protocol socket factory for a specific host:
 *
 * <pre>
 * Protocol authhttps = new Protocol(&quot;https&quot;, new AuthSSLProtocolSocketFactory(new URL(&quot;file:my.keystore&quot;), &quot;NoSoup4Uword&quot;,
 *         new URL(&quot;file:my.truststore&quot;), &quot;NoSoup4Uword&quot;), 443);
 *
 * HttpClient client = new HttpClient();
 * client.getHostConfiguration().setHost(&quot;localhost&quot;, 443, authhttps);
 * // use relative url only
 * GetMethod httpget = new GetMethod(&quot;/&quot;);
 * client.executeMethod(httpget);
 * </pre>
 *
 * </p>
 * <p>
 * Example of using custom protocol socket factory per default instead of the standard one:
 *
 * <pre>
 * Protocol authhttps = new Protocol(&quot;https&quot;, new AuthSSLProtocolSocketFactory(new URL(&quot;file:my.keystore&quot;), &quot;NoSoup4Uword&quot;,
 *         new URL(&quot;file:my.truststore&quot;), &quot;NoSoup4Uword&quot;), 443);
 * Protocol.registerProtocol(&quot;https&quot;, authhttps);
 *
 * HttpClient client = new HttpClient();
 * GetMethod httpget = new GetMethod(&quot;https://localhost/&quot;);
 * client.executeMethod(httpget);
 * </pre>
 *
 * </p>
 *
 *
 * <p>
 * DISCLAIMER: HttpClient developers DO NOT actively support this component. The component is provided as a reference
 * material, which may be inappropriate for use without additional customization.
 * </p>
 */

public class AuthSSLProtocolSocketFactory implements SecureProtocolSocketFactory {

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(AuthSSLProtocolSocketFactory.class);

    private final SSLContext sslcontext;

    /**
     * Constructor for AuthSSLProtocolSocketFactory. Either a keystore or truststore file must be given. Otherwise SSL
     * context initialization error will result.
     */
    public AuthSSLProtocolSocketFactory(KeyManager[] keymanagers, TrustManager[] trustmanagers) {
        super();
        this.sslcontext = createSSLContext(keymanagers, trustmanagers);
    }

    /**
     * Constructor for AuthSSLProtocolSocketFactory. Either a keystore or truststore file must be given. Otherwise SSL
     * context initialization error will result.
     */
    public AuthSSLProtocolSocketFactory(KeyStore truststore) {
        super();
        try {
            TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmfactory.init(truststore);
            this.sslcontext = createSSLContext(null, tmfactory.getTrustManagers());
        }
        catch (Exception e) {
            throw new RuntimeException("Cannot load truststore", e);
        }
    }

    private static TrustManager[] createTrustManagers(TrustManager[] trustmanagers) throws GeneralSecurityException {
        LOG.debug("Initializing trust manager");
        for (int i = 0; i < trustmanagers.length; i++) {
            if (trustmanagers[i] instanceof X509TrustManager) {
                trustmanagers[i] = new AuthSSLX509TrustManager((X509TrustManager) trustmanagers[i]);
            }
        }
        return trustmanagers;
    }

    private SSLContext createSSLContext(KeyManager[] keymanagers, TrustManager[] trustmanagers) {
        try {
            trustmanagers = createTrustManagers(trustmanagers);
            SSLContext sslcontext = SSLContext.getInstance("SSLv3");
            sslcontext.init(keymanagers, trustmanagers, null);
             return sslcontext;
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e.getMessage(), e);
            throw new AuthSSLInitializationException("Unsupported algorithm exception: " + e.getMessage());
        } catch (KeyStoreException e) {
            LOG.error(e.getMessage(), e);
            throw new AuthSSLInitializationException("Keystore exception: " + e.getMessage());
        } catch (GeneralSecurityException e) {
            LOG.error(e.getMessage(), e);
            throw new AuthSSLInitializationException("Key management exception: " + e.getMessage());
        }
        //return sslcontext;
    }

    /**
     * Attempts to get a new socket connection to the given host within the given time limit.
     * <p>
     * To circumvent the limitations of older JREs that do not support connect timeout a controller thread is executed.
     * The controller thread attempts to create a new socket within the given limit of time. If socket constructor does
     * not return until the timeout expires, the controller terminates and throws an {@link ConnectTimeoutException}
     * </p>
     *
     * @param host
     *            the host name/IP
     * @param port
     *            the port on the host
     * @param clientHost
     *            the local host name/IP to bind the socket to
     * @param clientPort
     *            the port on the local machine
     * @param params
     *            {@link HttpConnectionParams Http connection parameters}
     *
     * @return Socket a new socket
     *
     * @throws IOException
     *             if an I/O error occurs while creating the socket
     * @throws UnknownHostException
     *             if the IP address of the host cannot be determined
     */
    public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort,
            final HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        int timeout = params.getConnectionTimeout();
        SocketFactory socketfactory = sslcontext.getSocketFactory();
        if (timeout == 0) {
            SSLSocket socket = (SSLSocket) socketfactory.createSocket(host, port, localAddress, localPort);
            socket.setEnabledProtocols(new String[] { "SSLv3" });
            return socket;
        } else {
            SSLSocket socket = (SSLSocket) socketfactory.createSocket();
            SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
            SocketAddress remoteaddr = new InetSocketAddress(host, port);
            socket.bind(localaddr);
            socket.connect(remoteaddr, timeout);
            socket.setEnabledProtocols(new String[] { "SSLv3" });
            return socket;
        }
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
     */
    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException,
            UnknownHostException {
        SSLSocket socket = (SSLSocket) sslcontext.getSocketFactory().createSocket(host, port, clientHost,
                clientPort);
        socket.setEnabledProtocols(new String[] { "SSLv3" });
        return socket;
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
     */
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        SSLSocket socket = (SSLSocket) sslcontext.getSocketFactory().createSocket(host, port);
        socket.setEnabledProtocols(new String[] { "SSLv3" });
        return socket;
    }

    /**
     * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
     */
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException,
            UnknownHostException {
        SSLSocket sslSocket = (SSLSocket) sslcontext.getSocketFactory()
                .createSocket(socket, host, port, autoClose);
        sslSocket.setEnabledProtocols(new String[] { "SSLv3" });
        return sslSocket;
    }
}
