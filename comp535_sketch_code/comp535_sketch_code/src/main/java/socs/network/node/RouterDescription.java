package socs.network.node;

public class RouterDescription {
  //used to socket communication
  String processIPAddress;
  short processPortNumber;
  //used to identify the router in the simulated network space
  String simulatedIPAddress;
  //status of the router
  RouterStatus status;

  public RouterDescription(String processIPAddress, short processPortNumber,
                             String simulatedIPAddress, RouterStatus status) {
        this.processIPAddress = processIPAddress;
        this.processPortNumber = processPortNumber;
        this.simulatedIPAddress = simulatedIPAddress;
        this.status = status;
    }

    public String getProcessIPAddress() {
        return processIPAddress;
    }

    public short getProcessPortNumber() {
        return processPortNumber;
    }

    public String getSimulatedIPAddress() {
        return simulatedIPAddress;
    }

    public RouterStatus getStatus() {
        return status;
    }

    public void setProcessIPAddress(String processIPAddress) {
        this.processIPAddress = processIPAddress;
    }

    public void setProcessPortNumber(short processPortNumber) {
        this.processPortNumber = processPortNumber;
    }

    public void setSimulatedIPAddress(String simulatedIPAddress) {
        this.simulatedIPAddress = simulatedIPAddress;
    }

    public void setStatus(RouterStatus status) {
        this.status = status;
    }
}
