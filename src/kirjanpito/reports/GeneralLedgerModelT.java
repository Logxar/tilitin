package kirjanpito.reports;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.DTOCallback;
import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.DocumentType;
import kirjanpito.db.Entry;
import kirjanpito.db.EntryDAO;
import kirjanpito.db.Session;
import kirjanpito.util.AccountBalances;
import kirjanpito.util.CSVWriter;

/**
 * Malli pääkirja tositelajeittain -tulosteelle.
 * 
 * @author Tommi Helineva
 */
public class GeneralLedgerModelT extends GeneralLedgerModel {
	private List<DocumentType> documentTypes;

	public void run() throws DataAccessException {
		List<Document> documents;
		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		
		final HashMap<Integer, Document> documentMap =
			new HashMap<Integer, Document>();
		
		final HashMap<Integer, DocumentType> documentTypeMap =
			new HashMap<Integer, DocumentType>();
		
		final AccountBalances balances = new AccountBalances(registry.getAccounts());
		
		documentTypes = registry.getDocumentTypes();
		settings = registry.getSettings();
		rows = new ArrayList<GeneralLedgerRow>();
		
		try {
			sess = dataSource.openSession();
			documents = dataSource.getDocumentDAO(sess).getByPeriodId(period.getId(), 0);
			
			for (Document d : documents) {
				documentMap.put(d.getId(), d);
			}
			
			dataSource.getEntryDAO(sess).getByPeriodId(
				period.getId(), EntryDAO.ORDER_BY_DOCUMENT_NUMBER,
				new DTOCallback<Entry>() {
					public void process(Entry entry) {
						Account account = registry.getAccountById(entry.getAccountId());
						Document document = documentMap.get(entry.getDocumentId());
						
						if (account == null || document == null) {
							return;
						}
						
						balances.addEntry(entry);
						
						if (document.getDate().before(startDate) || document.getDate().after(endDate)) {
							return;
						}
						
						DocumentType type = getDocumentTypeByNumber(document.getNumber());
						
						if (type != null && !documentTypeMap.containsKey(account.getId())) {
							documentTypeMap.put(account.getId(), type);
						}
						
						lastDocumentNumber = Math.max(lastDocumentNumber, document.getNumber());
						rows.add(new GeneralLedgerRow(1, document, type, account, entry,
								balances.getBalance(account.getId())));
					}
				});
		}
		finally {
			if (sess != null) sess.close();
		}
		
		/* Asetetaan tositelaji sellaisille riveille, joille ei ole vielä asetettu
		 * tositelajia (esim. alkusaldoviennit). */
		for (GeneralLedgerRow row : rows) {
			if (row.documentType == null) {
				row.documentType = documentTypeMap.get(row.account.getId());
				
				if (row.documentType == null) {
					row.documentType = documentTypes.get(0);
				}
			}
		}
		
		Collections.sort(rows);
		int prevDocumentType = -1;
		int prevAccount = -1;
		
		for (int i = 0; i < rows.size(); i++) {
			GeneralLedgerRow row = rows.get(i);
			
			if (prevDocumentType != row.documentType.getId()) {
				if (prevDocumentType != -1) {
					rows.add(i++, new GeneralLedgerRow(0, null, null, null, null, null));
				}
				
				rows.add(i++, new GeneralLedgerRow(3, null, row.documentType, null, null, null));
				prevAccount = -1;
			}
			
			if (prevAccount != row.account.getId()) {
				rows.add(i++, new GeneralLedgerRow(0, null, null, null, null, null));
				rows.add(i++, new GeneralLedgerRow(2, null, null, row.account, null, null));
			}
			
			prevDocumentType = row.documentType.getId();
			prevAccount = row.account.getId();
		}
	}
	
	public void writeCSV(CSVWriter writer) throws IOException {
		writeCSV(writer, true);
	}

	/**
	 * Palauttaa käyttäjän nimen.
	 * 
	 * @return käyttäjän nimi
	 */
	public String getName() {
		return settings.getName();
	}
	
	/**
	 * Palauttaa Y-tunnuksen.
	 * 
	 * @return y-tunnus
	 */
	public String getBusinessId() {
		return settings.getBusinessId();
	}
	
	/**
	 * Palauttaa rivillä <code>index</code> olevan tositteen
	 * tositelajin
	 * 
	 * @param index rivinumero
	 * @return tositelaji
	 */
	public DocumentType getDocumentType(int index) {
		return rows.get(index).documentType;
	}
	
	private DocumentType getDocumentTypeByNumber(int number) {
		for (DocumentType type : documentTypes) {
			if (number >= type.getNumberStart() && number <= type.getNumberEnd()) {
				return type;
			}
		}
		
		return null;
	}
}