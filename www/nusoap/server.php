<?php
    require_once("lib/nusoap.php");

    $PASSWORD = 'zcy4201';  //服务器登录密码(全局变量)

    function checkPassword($pwd)
    {
        global $PASSWORD;
        if ($pwd === $PASSWORD)
        {
            //什么也不做
        }
        else
        {
            return false;
        }
    }

    function startServer($pwd, $name)
    {
        call_user_func('checkPassword', $pwd);

        //原型：string exec ( string command [, array &output [, int &return_var]] )
        //描述：返回值保存最后的输出结果，而所有输出结果将会保存到$output数组，$return_var用来保存命令执行的状态码（用来检测成功或失败）。
        $ret = exec("./".$name." >/dev/null &", $output, $var);

        return 'Server start up!';
    }

    function stopServer($pwd, $name)
    {
        // 每次都检查用户的密码保证安全
        call_user_func('checkPassword', $pwd);

        passthru("killall"." ".$name);
        return 'Server stop!';
    }

    function loadingServer($pwd)
    {
        global $PASSWORD;
        if ($pwd === $PASSWORD)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    function checkServer($pwd)
    {
        call_user_func('checkPassword', $pwd);

        // 读取new events
        $event_new = fopen("event_new", "r");
        if ($event_new) {
            while (!feof($event_new))
            {
               $line = fgets($event_new);
               $ret .= $line;
            }
            fclose($event_new);
            unlink("event_new");
        }

        // 保存events
        $event_save = fopen("event_save", "a+");
        fwrite($event_save, $ret);
        fclose($event_save);

        return $ret;
    }

    function loadImage($pwd, $filepath)
    {
        call_user_func('checkPassword', $pwd);

        // 将图像文件使用Base64编码并返回
        $handle = @fopen($filepath, "r");
        $getcontent = @fread($handle,filesize($filepath));
        fclose(@$handle);

        $content = @base64_encode($getcontent);

        return $content;
    }

    // 现阶段只有一份录制视频。。。 
    function loadVideo($pwd)
    {
        call_user_func('checkPassword', $pwd);

        // 将图像文件使用Base64编码并返回
        $filepath = "out.avi";
        $handle = @fopen($filepath, "r");
        $getcontent = @fread($handle,filesize($filepath));
        fclose(@$handle);

        $content = @base64_encode($getcontent);

        return $content;
    }

//////////////////////////////////////////////////////////////
    /**
     * SOCKET请求说明：
     * 0：获取帧
     * 1：开始录像
     * 2: 结束录像
     */
    // 从摄像头捕捉帧
    function captureFrame($pwd)
    {
        call_user_func('checkPassword', $pwd);

        // Socket通信
        $server_ip = "127.0.0.1";
        $port = 8888;

        $sock = @socket_create(AF_INET, SOCK_DGRAM, 0);

        if(!@socket_sendto($sock, "0", 1, 0, $server_ip, $port))
        {
            socket_close($sock);
            return;
        }

        $dataSize = 30 * 1024;
        if(!@socket_recvfrom($sock, $recvContent, $dataSize, 0, &$server_ip, &$port))
        {
            socket_close($sock);
            return;
        }

        socket_close($sock);

        // 将图像文件使用Base64编码并返回
        $content = @base64_encode($recvContent);

        return $content;
    }

    // 请求打开/关闭摄制录像开关
    function switchRecordVideo($pwd, $flag)
    {
        call_user_func('checkPassword', $pwd);

        // Socket通信
        $server_ip = "127.0.0.1";
        $port = 8888;

        $sock = @socket_create(AF_INET, SOCK_DGRAM, 0);

        $send = "";
        if ($flag == "true") $send = "1";  //开始录制标志
        else $send = "2";  //结束录制标志
        if(!@socket_sendto($sock, $send, 1, 0, $server_ip, $port))
        {
            socket_close($sock);
            return false;
        }

        // 请求应答
        if(!@socket_recvfrom($sock, $recv, 1, 0, &$server_ip, &$port))
        {
            socket_close($sock);
            return false;
        }

        socket_close($sock);

        if ($recv == '1') return true; //请求成功
        else return false;
    }


////////////////////////////////////////////////////////////////////

    function executeCommand($pwd, $command)
    {
        call_user_func('checkPassword', $pwd);

        passthru("./".$command);
        return 'Server start up!';
    }

    $soap = new soap_server();

    $soap->register('startServer');
    $soap->register('stopServer');
    $soap->register('loadingServer');
    $soap->register('checkServer');
    $soap->register('loadImage');
    $soap->register('loadVideo');
    $soap->register('captureFrame');
    $soap->register('switchRecordVideo');
    $soap->register('executeCommand');

    $soap->service($HTTP_RAW_POST_DATA);
?>
