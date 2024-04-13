package socs.network.message;

import java.io.Serializable;

public class LinkDescription implements Serializable {
  public String linkID;
  public int portNum;

    public LinkDescription() {}

    public LinkDescription(String pLinkID, int pPortNum) {
        this.linkID = pLinkID;
        this.portNum = pPortNum;
    }

  public String toString() {
    return linkID + ","  + portNum;
  }
}
