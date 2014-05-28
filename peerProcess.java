public class peerProcess {
    public static void main(String[] args){
    	if(args.length <= 0)
            System.out.println("peerProcess peerId");
        else{
        	Peer peer = new Peer(args[0]);
            try {
				peer.start();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            return;
        }
    }

}
