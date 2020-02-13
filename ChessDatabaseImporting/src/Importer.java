import java.util.ArrayList;
import java.io.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.regex.*;

import javax.swing.JFrame;


/**
 * This class handles data importing. It reads from a text file on local disk in standard FICS layout,
 * and puts people, players, matches, and moves into the database.
 * @author zonickba
 *
 */
public class Importer {

	private String file;
	private ChessbaseConnectionService chessdb;
	private Connection c;
	private final String dbName = "ChessDatabase";
	private final String serverName = "golem.csse.rose-hulman.edu";
	private int moveID;
	
	public static void main(String[] args) {
		new Importer();
	}
	
	/**
	 * Our constructor. We call a connection from here, and then tell our code to parse all of our games,
	 * before importing them.
	 */
	public Importer() {
		this.file = "c:/ficsgamesdb_201911_standard2000_nomovetimes_111149.pgn";
		this.moveID = 1;
		chessdb = new ChessbaseConnectionService(serverName, dbName);
		System.out.println(chessdb.connect("SodaBaseUserzonickba20", "Password123"));
		c = chessdb.getConnection();
		ArrayList<ChessGame> games = parseGames(this.file);
		importGames(games);
		chessdb.closeConnection();
	}
	
	
	/**
	 * Repeatedly calls 'importChessGame' for every game that we have parsed.
	 * @param games a set of parsed chess games to be imported.
	 */
	private void importGames(ArrayList<ChessGame> games) {
		
		for(int i = 0; i < games.size(); i++) {
			importChessGame(games.get(i), i+5);
		}
		
	}
	
	
	/**
	 * Our data parsing from file. This reads an FICS file, and stores the relevant items in variables, which
	 * it then places into new 'ChessGame' objects.
	 * @param file
	 * @return
	 */
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
			while(line != null && numGames < 100) { //The number on the right is the limiter. Note, there is a zero game, so doing "numGames < 100" imports 100 games, not 99
				cg = new ChessGame();
				count = 0;
				while(count < 2) { //A regular 'game' in the FICS format has one blank line in it. When we hit our second blank line (or EOF), we move on to the next game
					if(line.equals("")) count ++;
					String[] ss = line.split("\"");
					if(ss.length == 1) {
						if(!ss[0].equals("")) {
							System.out.println("Length 1: "+ ss[0]);
							cg.ms = ss[0];
							cg.setMoves();
						}
					}
					else if(ss.length != 0) { //This big case statement stores important lines into variables.
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
					//System.out.println(cg.toString());
					line = in.readLine();
					if(line == null) break; //For the last game, we hit EOF before a newline, so we break if we see EOF
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
	
	/**
	 * This imports a single chess game using stored procedures on the database side.
	 * @param c the game to be imported
	 * @param matchID the ID the game should be filed under.
	 */
	private void importChessGame(ChessGame c, int matchID) {
		
		System.out.println(c.whiteUser + " " +  c.blackUser);
		
		makePlayer(c.whiteUser, c.whiteIsComp); //We need to have valid players for this to work. By attempting to add them, we ensure that they exist. If they already exist, that's fine.
		makePlayer(c.blackUser, c.blackIsComp);
		
		String winnerUsername;
		String loserUsername;
		int wasDraw = 0;
		
		if(c.whiteResult.equals("1")) { //This part handles converting the FICS format for winners and losers to our format.
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
		
		makeGame(matchID, c.date, 9, 4, "FICS.org", winnerUsername, loserUsername, wasDraw); //This handles the actual stored procedure call for adding a game.
		
		makeMoves(c, matchID);
	}
	
	private void makeMoves(ChessGame c2, int matchID) {
		for(int i = 0; i < c2.moves.size(); i++) {
			addMove(c2.moves.get(i),  matchID, i);
		}
	}
	
	private boolean addMove(String move, int matchID, int turn) {
		CallableStatement cs;
		try {
			cs = c.prepareCall("{? = call ChessMoveInsert(?, ?, ?, ?, ?)}"); //The actual call.
			cs.setInt(2, moveID);
			cs.setInt(3, matchID);
			cs.setInt(4, turn);
			cs.setString(5, move);
			cs.setString(6, null);
			cs.registerOutParameter(1, Types.INTEGER);
			cs.execute();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		moveID++;
		return false;
	}

	/**
	 * This calls the stored procedure to add a game.
	 * @param matchID the ID of the match to add
	 * @param date the date on which the match happened.
	 * @param hostID the match's host ID. By default this is 9
	 * @param tournamentID the match's tournament ID. By default this is 4
	 * @param judgeUsername the username of the match's judge. By default this is "FICS.org"
	 * @param winnerUsername the winner's username. If it was a tie, white's username.
	 * @param loserUsername the loser's username. If it was a tie, black's username.
	 * @param wasDraw a 1 or 0 value indicating if there was a draw. If there was, winnerUsername indicates white's username, and loserUsername indicated black's username
	 * @return
	 */
	private boolean makeGame(int matchID, String date, int hostID, int tournamentID, String judgeUsername, String winnerUsername,
			String loserUsername, int wasDraw) {
		
		CallableStatement cs;
		
		String[] modDate = date.split("\\.");
		String fixDate = modDate[0]+"-"+modDate[1]+"-"+modDate[2]; //Date conversions. Always fun.
		System.out.println(fixDate);
		System.out.println(loserUsername);
		System.out.println(fixDate);
		
		try {
			cs = c.prepareCall("{? = call ChessMatchInsert(?, ?, ?, ?, ?, ?, ?, ?, ?)}"); //The actual call.
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

	/**
	 * Where we call the stored procedure to make players.
	 * @param player The player's username
	 * @param isComp whether the player is a computer or not. A boolean indicated by an int of value 0 or 1
	 * @return whether it worked.
	 */
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
	
	/**
	 * The call to make a person. This is mostly filled with 'fudged' data. Our dataset did not include
	 * values like passwords (obviously), so we give the person some fake data.
	 * @param person the person's username.
	 * @return whether it worked.
	 */
	private boolean makePerson(String person) {
		
		CallableStatement cs;
		
		try {
			cs = c.prepareCall("{? = call PersonInsert(?, ?, ?, ?)}");
			cs.setString(2, person); 
			cs.setString(3, "autogeneratedpassword");
			cs.setString(4, person); //Yes, we assume their real name is their username.
			cs.setString(5, "2/13/2020"); //Join date is fake, since we don't have access to FICS's data on that.
			cs.registerOutParameter(1, Types.INTEGER);
			cs.execute();
			return (0 == cs.getInt(1));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return false;
		
	}
	

	/**
	 * The class for a chess game. Mostly just a container for data.
	 * @author zonickba
	 *
	 */
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
		
		/**
		 * This method tells the object to break apart its string containing all of its moves into actual moves
		 */
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

