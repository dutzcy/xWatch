<?php
    // Socket的UDP通信类
    class Socket_UDP
    {
        var $server_ip;
        var $port;
        var $sock;

        function _construct($ip, $port)
        {
            $this->server_ip = $ip;
            $this->port = $port;
            
            $this->sock = @socket_create(AF_INET, SOCK_DGRAM, 0);
            if(!$this->sock)
                echo "socket create failure";
        }

        function sendto($sendbuf)
        {
                if(!@socket_sendto($sock, $buf, strlen($buf), 0, $server_ip, $port))
    {
        echo "send error\n";
        socket_close($sock);
        exit();
    }  
        }
    }

    $server_ip = "127.0.0.1";
    $port = 8888;

    echo "hello";

    $sock = @socket_create(AF_INET, SOCK_DGRAM, 0);
    if(!$sock)
        echo "socket create failure";

    if($buf == "")
        $buf = "hello,how are you!";

    if(!@socket_sendto($sock, $buf, strlen($buf), 0, $server_ip, $port))
    {
        echo "send error\n";
        socket_close($sock);
        exit();
    }  

    $buf = "";
    $msg = "";

    if(!@socket_recvfrom($sock, $msg, 256, 0, &$server_ip, &$port))
    {
        echo "recvieve error!";
        socket_close($sock);
        exit();
    }

    echo trim($msg)."\n";

    socket_close($sock);
?>
