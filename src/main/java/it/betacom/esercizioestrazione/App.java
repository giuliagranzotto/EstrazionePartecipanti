package it.betacom.esercizioestrazione;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Hello world!
 *
 */
public class App 
{
	public static final Logger logger = LogManager.getLogger("App");
	
    public static void main( String[] args )
    {
    	Connection con = null;
		Statement stm = null;
		ResultSet rs;
		int scelta;
		
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e1) {
			System.out.println("Errore nella ricerca del Driver JDBC: " + e1.getMessage());
			logger.error("Errore nella ricerca del Driver JDBC: " + e1.getMessage());
		}
		try {
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/esercizio_estrazioni","root","password");
			logger.info("Connessione al DB esercizio_estrazioni eseguita.");
			
			Scanner scanner = new Scanner(System.in);
			do
			{
				scelta = menu(scanner);
				logger.info("Scelta nel menu: " + scelta);
				
				stm = con.createStatement();
				String sql;
				
				switch(scelta)
				{
					case 1:	
						
						sql = "CREATE TABLE IF NOT EXISTS partecipante("
								+ "id_partecipante INT PRIMARY KEY NOT NULL AUTO_INCREMENT,"
								+ "nome VARCHAR(255) NOT NULL,"
								+ "sede VARCHAR(255) NOT NULL);";
						stm.executeUpdate(sql);
						logger.info("Tabella partecipante creata.");
						
						sql = "CREATE TABLE IF NOT EXISTS estrazioni("
								+ "id_estrazione INT PRIMARY KEY NOT NULL AUTO_INCREMENT,"
								+ "data_estrazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
								+ "id_partecipante INT NOT NULL,"
								+ "KEY id_partecipante (id_partecipante),"
								+ "CONSTRAINT id_partecipante FOREIGN KEY (id_partecipante) REFERENCES partecipante (id_partecipante));";
						stm.executeUpdate(sql);
						logger.info("Tabella estrazioni creata.");
						
						String filePath = "./esercizioPartecipanti.CSV";
				        BufferedReader br = new BufferedReader(new FileReader(filePath));
			            String line;
			            while ((line = br.readLine()) != null)
			            {
			                String[] columns = line.split(";");
			                String nomePartecipante = columns[0];
			                String sede = columns[1];
			                
			                sql = "INSERT INTO partecipante (nome, sede) VALUES ('" + nomePartecipante + "', '" + sede + "');";
			                stm.executeUpdate(sql);

			            }
			            
			            System.out.println("|      Tabelle create e dati inseriti con successo     |");
			            logger.info("Tabella partecipanti popolata.");
						
						break;
					
					case 2:
						
						rs = stm.executeQuery("SELECT COUNT(id_partecipante) FROM partecipante;");
						rs.next();
						int numPartecipanti = rs.getInt(1);
						
						Random generator = new Random();
						
						int estrazione = generator.nextInt(numPartecipanti) + 1;
						
						sql = "INSERT INTO estrazioni (id_partecipante) VALUES (" + estrazione + ");";
						stm.executeUpdate(sql);
						
						rs = stm.executeQuery("SELECT nome, sede FROM partecipante WHERE id_partecipante = " + estrazione + ";");
						rs.next();
						String nome, sede;
						nome = rs.getString("nome");
						sede = rs.getString("sede");
						
						System.out.println("| Partecipante estratto: " + nome + ", " + sede);
						logger.info("Partecipante estratto: " + nome + ", " + sede + ". Inserito nella tabella estrazioni.");
						
						break;
					
					case 3:
						
						rs = stm.executeQuery("SELECT COUNT(id_estrazione) AS numero_estrazioni, p.id_partecipante, p.nome, p.sede "
								+ "FROM estrazioni e INNER JOIN partecipante p ON e.id_partecipante = p.id_partecipante "
								+ "GROUP BY id_partecipante, p.nome, p.sede "
								+ "ORDER BY numero_estrazioni DESC;");
						
						while (rs.next())
						{
							int numero_estrazioni = rs.getInt("numero_estrazioni");
							int id_partecipante = rs.getInt("p.id_partecipante");
							String nome_partecipante = rs.getString("p.nome");
							String sede_partecipante = rs.getString("p.sede");
							 
							System.out.println("| ID: " + id_partecipante + ", " + nome_partecipante + ", " + sede_partecipante + " è stato/a estratto/a " + numero_estrazioni + " volte");
						}
						
						logger.info("Lista delle estrazioni, ordinate per numero di estrazioni decrescente, stampata.");

						break;
					
					case 4:
						
						rs = stm.executeQuery("SELECT COUNT(id_estrazione) AS numero_estrazioni, p.id_partecipante, p.nome, p.sede "
								+ "FROM estrazioni e INNER JOIN partecipante p ON e.id_partecipante = p.id_partecipante "
								+ "GROUP BY id_partecipante, p.nome, p.sede "
								+ "ORDER BY numero_estrazioni DESC;");
						
						try
						{
							Document document = new Document();
							FileOutputStream outputStream = new FileOutputStream(new File("./Estrazioni.pdf"));
							
							PdfWriter.getInstance(document, outputStream);
							
							document.open();
							
							String riga;
							
							riga = "- ID - Nome - Sede - Numero volte estratto -";
							document.add(new Paragraph(riga));
							
							riga = "-----------------------------------------------------------";
							document.add(new Paragraph(riga));
							
							
							while (rs.next())
							{
								int numero_estrazioni = rs.getInt("numero_estrazioni");
								int id_partecipante = rs.getInt("p.id_partecipante");
								String nome_partecipante = rs.getString("p.nome");
								String sede_partecipante = rs.getString("p.sede");
								
								riga = ("- " + id_partecipante + " - " + nome_partecipante + " - " + sede_partecipante + " - " + numero_estrazioni + " -");
								document.add(new Paragraph(riga));
							}
							
							document.close();
							outputStream.close();
							
							System.out.println("|  Documento PDF delle estrazioni creato con successo  |");
							logger.info("Documento PDF delle estrazioni creato.");
							
						} catch (DocumentException e) {
							System.out.println("Errore nell'apertura del documento PDF: " + e.getMessage());
							logger.error("Errore nell'apertura del documento PDF: " + e.getMessage());
						}
						
						break;
					
					case 5:
						
						sql = "DROP TABLE estrazioni;";
						stm.executeUpdate(sql);
						logger.info("Tabella estrazioni eliminata.");
						
						sql = "DROP TABLE partecipante;";
						logger.info("Tabella partecipante eliminata.");
						stm.executeUpdate(sql);
						
						System.out.println("|               Processo reinizializzato               |");
						logger.info("Processo reinizializzato.");
						
						break;
					
					case 0:
						
						System.out.println("|           Uscita dal programma in corso...           |");
						System.out.println("|------------------------------------------------------|");
						logger.info("Chiusura del programma.");
				}
				
			} while (scelta != 0);
			
			
			
		} catch (SQLException e) {
			System.out.println("Errore nella query/connessione al DB: " + e.getMessage());
			logger.error("Errore nella query/connessione al DB: " + e.getMessage());
		} catch (FileNotFoundException e) {
			System.out.println("Errore nella ricerca del file CSV: " + e.getMessage());
			logger.error("Errore nella ricerca del file CSV: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("Errore nell'input dei dati: " + e.getMessage());
			logger.error("Errore nell'input dei dati: " + e.getMessage());
		}
		
		finally {
			if(con != null)
			{
				try {
					con.close();
					logger.info("Chiusura della connessione col DB.");
				} catch (SQLException e) {
					System.out.println("Errore nella chiusura della connessione col DB: " + e.getMessage());
					logger.error("Errore nella chiusura della connessione col DB: " + e.getMessage());
				}
			}
			
			if(stm != null)
			{
				try {
					stm.close();
					logger.info("Chiusura dello statement.");
				} catch (SQLException e) {
					System.out.println("Errore nella chiusura dello statement: " + e.getMessage());
					logger.error("Errore nella chiusura dello statement: " + e.getMessage());
				}
			}
		}
    }
    
    public static int menu(Scanner scanner)
    {
    	int scelta;
    	System.out.println("|------------------------------------------------------|");
    	System.out.println("| 1. Inizializzare                                     |");
    	System.out.println("| 2. Estrazione                                        |");
    	System.out.println("| 3. Stampa situazione estrazioni                      |");
    	System.out.println("| 4. Scrittura su file pdf della situazione estrazioni |");
    	System.out.println("| 5. Reinizializzazione del processo                   |");
    	System.out.println("| 0. Esci                                              |");
    	System.out.println("|------------------------------------------------------|");
    	System.out.print("| Inserisci l'operazione: ");
    	do
    	{
	    	scelta = scanner.nextInt();
	    	if(scelta < 0 || scelta > 5)
			{
				System.out.print("| Operazione inserita non valida. Reinseriscila: ");
				logger.warn("Operazione inserita non valida. Valore inserito: " + scelta);
			}
    	} while(scelta < 0 || scelta > 5);
    	System.out.println("|------------------------------------------------------|");
    	
    	return scelta;
    }
}
