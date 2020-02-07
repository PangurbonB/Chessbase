import java.util.ArrayList;
import java.io.*;
import java.util.regex.*;

import javax.swing.JFrame;

public class Importer {

	private String file;
	private ChessbaseConnectionService chessdb;
	private final String dbName = "ChessDatabase";
	private final String serverName = "golem.csse.rose-hulman.edu";
	
	public static void main(String[] args) {
		new Importer();
	}
	
	public Importer() {
		this.file = "c:/ficsgamesdb_201911_standard2000_nomovetimes_111149.pgn";
		chessdb = new ChessbaseConnectionService(serverName, dbName);
		ArrayList<ChessGame> games = parseGames(this.file);
	}
	
	private boolean importGames(ArrayList<ChessGame> games) {
		
		
		for(ChessGame g : games) {
			
		}
		
		
		
		return false;
		
	}
	
	private ArrayList<ChessGame> parseGames(String file) {
		
		BufferedReader in = null;
		ArrayList<String> lines = new ArrayList<String>();
		int count = 0;
		String line = "";
		int numGames = 0;
		
		ChessGame cg;
		try {
			in = new BufferedReader(new FileReader(file));
			cg = new ChessGame();
			
			line = in.readLine();
			while(line != null/* && numGames < 1000*/) {
				count = 0;
				while(count < 2) {
					if(line.equals("")) count ++;
					String[] ss = line.split("\"");
					if(ss.length == 1) {
						if(!ss[0].equals("")) {
							//System.out.println("Length 1: "+ ss[0]);
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
					line = in.readLine();
					if(line == null) break;
				}
				System.out.println(numGames);
				numGames++;
			}
			
		}catch(FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		
		
		return null;
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

