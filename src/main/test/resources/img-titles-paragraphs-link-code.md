[IMG1]()

# Vaadin TestBench

Vaadin TestBench is both a unit testing tool and an integration testing tool.

The unit testing tool does not rely on a browser, so its tests run very fast. Because these tests do not run a browser, the browser's Javascript is not available.

The integration testing tool is built on Selenium and is used for automated user interface testing of web applications on multiple platforms and browsers.

Choosing testing tools optimized for Vaadin UI testing and supported by Vaadin provides you with the best integration and upward compatibility with the Vaadin framework. 

## Releases

Official releases of this add-on are available at
[https://vaadin.com/directory/component/vaadin-testbench](https://vaadin.com/directory/component/vaadin-testbench).

## Building TestBench

    git clone https://github.com/vaadin/testbench.git
    cd testbench
    mvn clean install
