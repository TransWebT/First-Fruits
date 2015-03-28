package us.wardware.firstfruits;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.bson.types.ObjectId;
import us.wardware.firstfruits.util.DateUtils;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;

public class RecordManager extends Observable implements Observer
{
    private static RecordManager INSTANCE = new RecordManager();
    private List<GivingRecord> records;
    private Set<String> uniqueLastNames;
    private Map<String, Set<String>> firstNamesForLastName;
    private GivingRecord selectedRecord;
    private boolean unsavedChanges;
    private int selectionCount;
    private GivingRecord lastUpdated;
    private String selectedDate;
    private RecordFilter recordFilter;
    
    private MongoClient client = null;
    private DB database = null;
    private DBCollection collection = null;

    public static RecordManager getInstance()
    {
        return INSTANCE;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private RecordManager()
    {
        uniqueLastNames = new HashSet<String>();
        uniqueLastNames.add("");
        uniqueLastNames.add("Anonymous");
        firstNamesForLastName = new HashMap<String, Set<String>>();
        records = new ArrayList<GivingRecord>();
        unsavedChanges = false;
        recordFilter = new RecordFilter();
        Settings.getInstance().addObserver(this);

		if (Settings.getInstance().getStringValue(Settings.DATABASE_SERVER)==null ||
			Settings.getInstance().getStringValue(Settings.DATABASE_SERVER).length()<1 ||
			Settings.getInstance().getStringValue(Settings.DATABASE_PORT)==null ||
			Settings.getInstance().getStringValue(Settings.DATABASE_PORT).length()<1 ||
			Settings.getInstance().getStringValue(Settings.DATABASE_NAME)==null ||
			Settings.getInstance().getStringValue(Settings.DATABASE_NAME).length()<1 ||
			Settings.getInstance().getStringValue(Settings.DATABASE_COLLECTION_NAME)==null ||
			Settings.getInstance().getStringValue(Settings.DATABASE_COLLECTION_NAME).length()<1
			)
		{
            System.out.println("MongoDB database configuration not set - skipping.");
            return;
		}

		try {
            client = new MongoClient(new ServerAddress(
                                Settings.getInstance().getStringValue(Settings.DATABASE_SERVER),
                                Integer.parseInt(Settings.getInstance().getStringValue(Settings.DATABASE_PORT))));
            database = client.getDB(Settings.getInstance().getStringValue(Settings.DATABASE_NAME));
            setCollection(database.getCollection(Settings.getInstance().getStringValue(Settings.DATABASE_COLLECTION_NAME)));
            // ToDo: set this in order handle file save messages on exit
            // setDatasourceType(Settings.getInstance().DatasourceType.MONGODB);

            /*
			client = new MongoClient(new ServerAddress("localhost", 27017));
	        database = client.getDB("FirstFruits");
	        setCollection(database.getCollection("offerings"));
	        */
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }

    public void updateRecord(GivingRecord record)
    {
        unsavedChanges = true;
        if (selectedRecord != null) {
            selectedRecord.update(record);
            lastUpdated = selectedRecord;
            updateDbRecord(record);
            setChanged();
            notifyObservers(selectedRecord);
        } else {
            lastUpdated = record;
            records.add(record);
            insertDbRecord(record);
            setChanged();
            notifyObservers(records);
            if (!uniqueLastNames.contains(record.getLastName())) {
                uniqueLastNames.add(record.getLastName().trim());
                setChanged();
                notifyObservers(uniqueLastNames);
            }
            
            updateFirstNamesForLastName(record.getLastName(), record.getFirstName());
        }
    }

    // ToDo: for consistency, review organization of database-related I/O code in RecordManager, GivingRecordsReader and GivingRecordsWriter.
    // Update existing MongoDB document
    private void updateDbRecord(GivingRecord record)
    {
        if (collection == null) {
            System.out.println("No available database collection - skipping MongoDB archive.");
            return;
        }
        if (record.getId() == null || record.getId().length() < 1) {
            System.out.println("Request to save existing record to the database without a valid record Id.");
            return;
        }

        BasicDBObject dbAmountsObject = new BasicDBObject();
        for (String category : Settings.getInstance().getCategories()) {
            if (record.getCategorizedAmounts().containsKey(category)) {
                dbAmountsObject.append(category, record.getAmountForCategory(category));
            }
        }

        DBObject dbRecord = new BasicDBObject("dateString", record.getDateString())
                .append("lastName", record.getLastName())
                .append("firstName", record.getFirstName())
                .append("fundType", record.getFundType())
                .append("checkNumber", record.getCheckNumber())
                .append("categorizedAmounts", dbAmountsObject);
        try {
            collection.update(new BasicDBObject("_id", record.getId()), dbRecord, false, false);
        } catch (MongoException.DuplicateKey e) {
            System.out.println("Offering Id already in use: " + dbRecord);
        }
    }

    // Insert existing MongoDB document
    private void insertDbRecord(GivingRecord record)
    {
        if (collection == null) {
            System.out.println("No available database collection - skipping MongoDB archive.");
            return;
        }
        if (record.getId() != null && record.getId().length() > 0) {
            System.out.println("Request to insert a duplicate copy of an existing record to the database.");
            return;
        }

        BasicDBObject dbAmountsObject = new BasicDBObject();
        for (String category : Settings.getInstance().getCategories()) {
            if (record.getCategorizedAmounts().containsKey(category)) {
                dbAmountsObject.append(category, record.getAmountForCategory(category));
            }
        }

        DBObject dbRecord = new BasicDBObject("dateString", record.getDateString())
                .append("lastName", record.getLastName())
                .append("firstName", record.getFirstName())
                .append("fundType", record.getFundType())
                .append("checkNumber", record.getCheckNumber())
                .append("categorizedAmounts", dbAmountsObject);
        try {
            collection.insert(dbRecord);
            record.setId(dbRecord.get("_id").toString());
        } catch (MongoException.DuplicateKey e) {
            System.out.println("Offering Id already in use: " + dbRecord);
        }
    }

    public GivingRecord getLastUpdatedRecord()
    {
        return lastUpdated;
    }

    public GivingRecord getSelectedRecord()
    {
        return selectedRecord;
    }

    public void setSelectedRecord(GivingRecord record)
    {
        this.selectedRecord = record;
        setChanged();
        notifyObservers();
    }

    public List<String> getUniqueLastNames()
    {
        final List<String> namesSorted = new ArrayList<String>(uniqueLastNames);
        Collections.sort(namesSorted);
        return namesSorted;
    }

    public void setUniqueNames(Set<String> names)
    {
        uniqueLastNames = names;
        setChanged();
        notifyObservers(uniqueLastNames);
    }

    public void setRecords(List<GivingRecord> records)
    {
        this.records = records;
        final Set<String> names = new HashSet<String>();
        names.add("");
        names.add("Anonymous");
        updateFirstNamesForLastName("", "");
        for (GivingRecord record : records) {
            names.add(record.getLastName().trim());
            updateFirstNamesForLastName(record.getLastName(), record.getFirstName());
        }
        setUniqueNames(names);
        setChanged();
        notifyObservers(records);
    }

    public List<GivingRecord> getAllRecords()
    {
        return records;
    }
    
    public List<GivingRecord> getRecordsForDate(String date)
    {
        final List<GivingRecord> recordsForSelectedDate = new ArrayList<GivingRecord>();
        for (GivingRecord record : records)
        {
            if (date != null && DateUtils.areEqualDateStrings(date, record.getDateString())) {
                recordsForSelectedDate.add(record);
            }
        }
        return recordsForSelectedDate;
    }

    public List<GivingRecord> getRecords()
    {
        return records;
    }
    
    public boolean hasUnsavedChanges()
    {
        return unsavedChanges;
    }

    public void setUnsavedChanges(boolean value)
    {
        unsavedChanges = value;
        setChanged();
        notifyObservers(unsavedChanges);
    }

    public List<GivingRecord> getRecordsForName(String lastName, String firstName)
    {
        final List<GivingRecord> recordsForName = new ArrayList<GivingRecord>();
        for (GivingRecord record : records) {
            if (record.getLastName().equals(lastName) &&
                record.getFirstName().equals(firstName)) {
                recordsForName.add(record);
            }
        }
        return recordsForName;
    }

    public void deleteRecords(List<GivingRecord> toRemove)
    {
        if (records.removeAll(toRemove)) {
            unsavedChanges = true;
            setChanged();
            notifyObservers(records);
        }
    }

    // ToDo: move this and other db operations to GivingRecordsReader/GivingRecordsWriter for consistency
    // Delete one or more existing MongoDB documents
    public void deleteDbRecords(List<GivingRecord> toRemove)
    {
        if (collection == null) {
            System.out.println("No available database collection - skipping MongoDB deletion.");
            return;
        }

        for (GivingRecord record : toRemove) {
            if (record.getId() == null || record.getId().length() < 1) {
                System.out.println("Request to save existing record to the database without a valid record Id.");
                continue;
            }
            BasicDBObject removeObject = new BasicDBObject("_id", new ObjectId(record.getId()));
            System.out.println("Deleting object: " + removeObject.toString());
            collection.remove(removeObject);
        }

        // Remove items from in-memory list
        deleteRecords(toRemove);
    }



    public int getSelectionCount()
    {
        return selectionCount;
    }

    public void setSelectionCount(int selectionCount)
    {
        this.selectionCount = selectionCount;
        setChanged();
        notifyObservers(selectionCount);
    }

    public void createNew()
    {
        records.clear();
        uniqueLastNames.clear();
        uniqueLastNames.add("");
        uniqueLastNames.add("Anonymous");
        firstNamesForLastName.clear();        
        selectedRecord = null;
        selectionCount = 0;
        unsavedChanges = false;
        setChanged();
        notifyObservers(records);
    }

    public void setSelectedDate(String date)
    {
        setChanged();
        selectedDate = date;
        notifyObservers(selectedDate);
    }

    public String getSelectedDate()
    {
        return selectedDate;
    }
    
    public RecordFilter getRecordFilter()
    {
        return recordFilter;
    }
    
    public void setRecordFilter(RecordFilter filter)
    {
        recordFilter = filter;
        setChanged();
        notifyObservers(recordFilter);
    }

    @Override
    public void update(Observable arg0, Object arg1)
    {
        final List<String> categories = Settings.getInstance().getCategories();
        for (GivingRecord record : records) {
            record.updateCategories(categories);
        }
    }

    public List<String> getFirstNamesForLastName(String lastName)
    {
        if (firstNamesForLastName.containsKey(lastName)) {
            final List<String> namesSorted = new ArrayList<String>(firstNamesForLastName.get(lastName));
            Collections.sort(namesSorted);
            return namesSorted;
        }
        return new ArrayList<String>();
    }
    
    public void updateFirstNamesForLastName(String lastName, String firstName)
    {
        if (!firstNamesForLastName.containsKey(lastName)) {
            firstNamesForLastName.put(lastName, new HashSet<String>());
        }

        firstNamesForLastName.get(lastName).add(firstName);
    }

    public List<String> getReportNameList()
    {
        final List<String> reportNames = new ArrayList<String>();
        reportNames.add("");
        for (String lastName : firstNamesForLastName.keySet()) {
            if (!lastName.isEmpty()) {
                for (String firstName : firstNamesForLastName.get(lastName)) {
                    reportNames.add(lastName + ", " + firstName);
                }
            }
        }
        return reportNames;
    }

    public boolean hasRecords()
    {
        return !records.isEmpty();
    }

    public void renameCategory(String oldName, String newName)
    {
        for (GivingRecord record : records) {
            record.renameCategory(oldName, newName);
        }
        if (hasRecords()) {
            unsavedChanges = true;
        }
    }

    public void removeCategory(String category)
    {
        for (GivingRecord record : records) {
            record.removeCategory(category);
        }
        if (hasRecords()) {
            unsavedChanges = true;
        }
    }

	public DBCollection getCollection() {
		return collection;
	}

	public void setCollection(DBCollection collection) {
		this.collection = collection;
	}
}
