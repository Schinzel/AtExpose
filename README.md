# @Expose

The goal of @Expose is to be the least distracting and fastest way to get a web server up and 
running that can serve static file content such as HTML-files and handle proprietary Ajax requests. 

Put differently: It should be the web server that distracts you the least from solving the actual problem you are working on.

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/20d9426304f246c18f22402af9cb22bb)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Schinzel/atexpose&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/20d9426304f246c18f22402af9cb22bb)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Schinzel/atexpose&amp;utm_campaign=Badge_Coverage)
[![Build Status](https://travis-ci.org/Schinzel/atexpose.svg?branch=master)](https://travis-ci.org/Schinzel/atexpose)

There are other frameworks and projects that offers similar functionality to
@Expose. Many of them are a lot more well used and tested than @Expose. Notable
examples are GlassFish and Jetty. If in doubt use these instead. 


##### Easy to get started
Clear and well documented samples included.

##### Intuitive to use
```
AtExpose.create().getWebServerBuilder()
        .port(8080)
        .webServerDir("mydir")
        .startWebServer();

Add a task that runs once per day:
addDailyTask MyTaskName, 'downloadSomething', 11:40
```
##### A large degree freedom
* You want ten different web servers, you got it. 
* Extensible. You can build you own custom parsers, wrappers, channels and more.  


##### Battle Tested
@Expose runs well on Heroku and is battle tested on a site that has thousands of visitors per day.


