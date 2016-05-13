<?php
    require_once("lib/nusoap.php");

    $client = new nusoap_client("http://127.0.0.1/nusoap/server.php");

    $pwd = 'zcy4201';
    $params = array('a'=>$pwd, 'b'=>'10240', 'c'=>'1001024');
    $str = $client->call('loadVideo', $params);
    echo $str;

    /*if (!$err=$client->getError())
    {
        echo " 程序返回 :",$str;
    } else {
        echo " 错误 :",$err;
    }
    //下面显示request和response 变量的内容
    echo '<p/>'; echo 'Request:';
    echo'<pre>',htmlspecialchars($client->request,ENT_QUOTES),'</pre>';
    echo 'Response:';
    echo '<pre>',htmlspecialchars($client->response,ENT_QUOTES ),'</pre>';

?>
