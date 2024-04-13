# Link-State-Routing
Steps to Run
- Install Maven: https://maven.apache.org/install.html
- mvn compile in "comp535_sketch_code/comp535_sketch_code"
- mvn compile assembly:single
- To start router1: java -cp target/COMP535-1.0-SNAPSHOT-jar-with-dependencies.jar socs.network.Main conf/router1.conf
- To attach: attach 127.0.0.1 894 192.168.1.100


Simple demo for attach, start and lsu:

- In 1 terminal for router 1: 
java -cp target/COMP535-1.0-SNAPSHOT-jar-with-dependencies.jar socs.network.Main conf/router1.conf

- In 1 terminal for router 2:
java -cp target/COMP535-1.0-SNAPSHOT-jar-with-dependencies.jar socs.network.Main conf/router2.conf

- In 1 terminal for router 1:
attach <ip router2> <port router2> 192.168.1.100
start
print lsd
