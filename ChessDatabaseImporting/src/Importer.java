import java.util.ArrayList;
import java.io.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.regex.*;

import javax.swing.JFrame;

public class Importer {

	private String file;
	private ChessbaseConnectionService chessdb;
	private Connection c;
	private final String dbName = "ChessDatabase";
	private final String serverName = "golem.csse.rose-hulman.edu";
	
	public static void main(String[] args) {
		new Importer();
	}
	
	public Importer() {
		this.file = "c:/ficsgamesdb_201911_standard2000_nomovetimes_111149.pgn";
		chessdb = new ChessbaseConnectionService(serverName, dbName);
		System.out.println(chessdb.connect("SodaBaseUserzonickba20", "Password123"));
		c= chessdb.getConnection();
		ArrayList<ChessGame> games = parseGames(this.file);
		importGames(games);
		chessdb.closeConnection();
	}
	
	private boolean importGames(ArrayList<ChessGame> games) {
		
		
		
		for(int i = 0; i < games.size(); i++) {
			importChessGame(games.get(i), i+5);
		}
		
		return true;
		
	}
	
	private ArrayList<ChessGame> parseGames(String file) {
		
		BufferedReader in = null;
		ArrayList<String> lines = new ArrayList<String>();
		int count = 0;
		String line = "";
		int numGames = 0;
		ArrayList<ChessGame> cgs = new ArrayList<ChessGame>();
		
		ChessGame cg;
		try {
			in = new BufferedReader(new FileReader(file));
			
			
			line = in.readLine();
			while(line != null && numGames < 100) {
				cg = new ChessGame();
				count = 0;
				while(count < 2) {
					if(line.equals("")) count ++;
					String[] ss = line.split("\"");
					if(ss.length == 1) {
						if(!ss[0].equals("")) {
							System.out.println("Length 1: "+ ss[0]);
							cg.ms = ss[0];
							cg.setMoves();
						}
					}
					else if(ss.length != 0) {
						switch(ss[0]) {
						case "[Event ": cg.event = ss[1]; break;
						case "[Site ": cg.site = ss[1]; break;
						case "[FICSGamesDBGameNo ": cg.gameNo = Integer.parseInt(ss[1]); break;
						case "[White ": cg.whiteUser = ss[1]; break;
						case "[Black ": cg.blackUser = ss[1]; break;
						case "[WhiteElo ": cg.whiteElo = Integer.parseInt(ss[1]); break;
						case "[BlackElo ": cg.blackElo = Integer.parseInt(ss[1]); break;
						case "[WhiteRD ": cg.whiteRD = Double.parseDouble(ss[1]); break;
						case "[BlackRD ": cg.blackRD = Double.parseDouble(ss[1]); break;
						case "[BlackIsComp ": cg.blackIsComp = (ss[1].equals("Yes")); break;
						case "[WhiteIsComp ": cg.whiteIsComp = (ss[1].equals("Yes")); break;
						case "[Date ": cg.date = ss[1]; break;
						case "[Time ": cg.time = ss[1]; break;
						case "[PlyCount ": cg.playCount = Integer.parseInt(ss[1]); break;
						case "[Result ": String[] s2 = ss[1].split("-"); cg.whiteResult = s2[0]; cg.blackResult = s2[1]; break;
						}
					}
					System.out.println(cg.toString());
					line = in.readLine();
					if(line == null) break;
				}
				System.out.println(numGames);
				cgs.add(cg);
				numGames++;
			}
			
			
		}catch(FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		
		
		return cgs;
	}
	
	
	//FICS INFO:
	//Tournament ID: 4
	//Host ID: 9
	//Judge Username: FICS.org
	private void importChessGame(ChessGame c, int matchID) {
		
		System.out.println(c.whiteUser + " " +  c.blackUser);
		
		makePlayer(c.whiteUser, c.whiteIsComp);
		makePlayer(c.blackUser, c.blackIsComp);
		
		String winnerUsername;
		String loserUsername;
		int wasDraw = 0;
		
		if(c.whiteResult.equals("1")) {
			winnerUsername = c.whiteUser;
			loserUsername = c.blackUser;
		}
		else if(c.blackResult.equals("1")) {
			winnerUsername = c.blackUser;
			loserUsername = c.blackUser;
		}
		else {
			winnerUsername = c.whiteUser;
			loserUsername = c.blackUser;
			wasDraw = 1;
		}
		
		makeGame(matchID, c.date, 9, 4, "FICS.org", winnerUsername, loserUsername, wasDraw);
		
	}
	
	private boolean makeGame(int matchID, String date, int hostID, int tournamentID, String judgeUsername, String winnerUsername,
			String loserUsername, int wasDraw) {
		
		CallableStatement cs;
		
		String[] modDate = date.split("\\.");
		String fixDate = modDate[0]+"-"+modDate[1]+"-"+modDate[2];
		System.out.println(fixDate);
		System.out.println(loserUsername);
		System.out.println(fixDate);
		
		try {
			cs = c.prepareCall("{? = call ChessMatchInsert(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
			cs.setInt(2, matchID);
			cs.setString(3, fixDate);
			cs.setString(4, fixDate);
			cs.setInt(5, hostID);
			cs.setInt(6, tournamentID);
			cs.setString(7, judgeUsername);
			cs.setString(8, winnerUsername);
			cs.setString(9, loserUsername);
			cs.setInt(10, wasDraw);
			cs.registerOutParameter(1, Types.INTEGER);
			cs.execute();
			return (0 == cs.getInt(1));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
		
	}

	private boolean makePlayer(String player, boolean isComp) {
		
		makePerson(player);
		
		CallableStatement cs;
		
		int comp;
		if(isComp) {
			comp = 1;
		}
		else {
			comp = 0;
		}
		
		try {
			cs = c.prepareCall("{? = call PlayerInsert(?, ?, ?)}");
			cs.setString(2, player);
			cs.setInt(3, (int) (2500 * Math.random()));
			cs.setInt(4, comp);
			cs.registerOutParameter(1, Types.INTEGER);
			cs.execute();
			return (0 == cs.getInt(1));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	private boolean makePerson(String person) {
		
		CallableStatement cs;
		
		try {
			cs = c.prepareCall("{? = call PersonInsert(?, ?, ?, ?)}");
			cs.setString(2, person);
			cs.setString(3, "autogeneratedpassword");
			cs.setString(4, person);
			cs.setString(5, "2/13/2020");
			cs.registerOutParameter(1, Types.INTEGER);
			cs.execute();
			return (0 == cs.getInt(1));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return false;
		
	}
	

	class ChessGame {
		String event;
		String site;
		int gameNo;
		String whiteUser;
		String blackUser;
		int whiteElo;
		int blackElo;
		double whiteRD;
		double blackRD;
		boolean whiteIsComp;
		boolean blackIsComp;
		String date;
		String time;
		int playCount;
		String whiteResult;
		String blackResult;
		String ms;
		ArrayList<String> moves;
		
		ChessGame(){
			moves = new ArrayList<String>();
		}
		
		@Override
		public String toString(){
			String s = "\n";
			//setMoves();
			return event+s+site+s+gameNo+s+whiteUser+s+blackUser+s+whiteElo+s+blackElo+s+whiteRD+s+blackRD+s+whiteIsComp+s+blackIsComp+s+
					date+s+time+s+playCount+s+whiteResult+s+blackResult+s+moves;
		}
		
		public void setMoves() {
			String[] trimmed = ms.split("\\{");
			String[] brokenUp = trimmed[0].split("\\.");
			for(int i = 1; i < brokenUp.length; i++) {
				String[] playerMoves = brokenUp[i].split(" ");
				moves.add((i-1)*2, playerMoves[1]);
				if(playerMoves.length > 2) {
					moves.add((i-1)*2+1, playerMoves[2]);
				}
			}
		}
	}

	
	
}

