# Link-State-Routing
Steps to Run
- Install Maven: https://maven.apache.org/install.html
- mvn compile in "comp535_sketch_code/comp535_sketch_code"
- mvn compile assembly:single
- To start router1: java -cp target/COMP535-1.0-SNAPSHOT-jar-with-dependencies.jar socs.network.Main conf/router1.conf
- To attach: attach <processIP> <port> <simulated ip>