# SmartThings Handlers and SmartApps
Personal Repo of custom SmartThings Handlers and SmartApps.

### Handler: Xiaomi Mijia Smart Switch

This handler allows for single, double, triple and quadruple clicks. It's built based on code found on [bspranger repo here](https://github.com/bspranger/Xiaomi/blob/master/devicetypes/bspranger/xiaomi-button.src/xiaomi-button.groovy).

I have made this fork to learn about Smart Things while also adding functionality I thought was missing from the Mijia Switch.

I was looking primarily into adding multi-click functionality and in the end a lot of code for other non-essential functionality was removed as I personally didn't find useful. E.g. Button held functionality which didn't work so well for me.

This is the exact button I'm using which works well with multi-clicks: https://uk.gearbest.com/smart-light-bulb/pp_257679.html?wid=1433363

<img src="https://gloimg.gbtcdn.com/soa/gb/pdm-product-pic/Electronic/2018/11/24/goods_img_big-v3/20181124114115_30024.jpg" alt="" data-canonical-src="https://uk.gearbest.com/smart-light-bulb/pp_257679.html?wid=1433363" width="150" height="150" />

### SmartApp: 1-2-3 Button Click Controller

The SmartApp is used to control 3 lights using a single button.

1, 2 or 3 clicks determine which of the 3 lights will switch on. When that happens other lights are switched off.

4 Clicks will switch off all lights. Also, if the number of clicks matches a light that is already switched on, then it will switch off all lights.
