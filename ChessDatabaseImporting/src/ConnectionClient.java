/**
 * Makes a connection client
 * @author zonickba
 *
 */
public class ConnectionClient {
	
	ChessbaseConnectionService chessdb;
	private final String dbName = "ChessDatabase";
	private final String serverName = "golem.csse.rose-hulman.edu";

	public static void main(String[] args) {
		new ConnectionClient();
	}
	
	public ConnectionClient() {
		chessdb = new ChessbaseConnectionService(serverName, dbName);
	}
}
