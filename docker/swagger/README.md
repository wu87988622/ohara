#  Swagger doc 
### *Command in comment*
啟動swagger docker  

    run swagger
啟動swagger 和 configurator (測試API)
    
    run swagger -c

使用swagger 提供的編輯器來拿github 的configurator.yaml
    
    run swagger -l
- - - 
#
### *I wanna build*
>swagger build 將需要的檔案copy 進去 image中
 所以每次檔案修改 需要重build 或是 用 run 時的 -v 來mount檔案以動態存取

    #docker build 的路徑是在 ohara/docker
    docker build -f swagger.dockerfile -t swagger . --no-cache

- - - 
#
### *I wanna view*  
靜態 - 修改需要重build 才會呈現(使用原始image中的yaml檔案) 

    docker run -p 8080:80 swagger

動態 - 所有修改將會呈現(使用 -v 參數 在ohara/docker 中下以下指令)
    
    docker run -p 8080:80 -v ${PWD}/swagger/yamls/:/usr/share/nginx/html/yamls/ swagger

瀏覽器 開啟 http://localhost:8080/  
- - - 
#
### *I wanna edit(手動儲存)* 
線上編輯 https://editor.swagger.io/
>進入網頁
import file  - 把ohara/docker/swagger/yaml  底下要編輯的檔案 載入
開始編輯
編輯完 記得手動save回檔案
#
- - - 
#
### *I wanna test*

run configurator (see ohara/readme.md)
    
    docker run --rm -p 12345:12345 islandsystems/configurator:0.2-SNAPSHOT --port 12345
run swagger with env(CONFIGURATORURL)  
    
    docker run -e CONFIGURATORURL=10.100.0.117:12345 -p 8080:80 swagger

>瀏覽器 開啟 http://localhost:8080/
展開你想要測試的API  
點選try it out 
填寫參數 或 不用參數
點選excute 執行
看response的欄位 會有結果

- - - 
#
### yaml 結構
https://swagger.io/docs/specification/basic-structure/
