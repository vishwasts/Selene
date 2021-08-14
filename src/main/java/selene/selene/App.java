package selene.selene;


import java.io.*;
import java.sql.*;
import java.util.*;
import org.openqa.selenium.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.edge.*;
import org.openqa.selenium.firefox.*;
import org.openqa.selenium.opera.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

//Class to store the company and roles specifies
class Struct
{
	String company;
	String role;
}


class Scraper
{
	private Connection con; // Variable that holds the SQL connection 
	private WebDriver driver; 
	//WebDriverWait wait;
	ArrayList<Struct> list; // ArrayList to store the roles and companies retrieved from CompAndRoles.txt
	ArrayList<String> urls; // ArrayList to store the URLS retrieved from the google search
	
	//Construction that instantiates the ArrayLists list and URLS
	Scraper()
	{
		//wait = new WebDriverWait(driver, 30);
		list = new ArrayList<Struct>();
		urls = new ArrayList<String>();
	}
	
	//Runner function that runs all the necessary function in order
	void run()
	{
		readList();
		configureWebDriver();
		//login();
		search();
		connectSql();
		writeToDB();
	}
	
	//Gets the user input on the choice of the browser(Chrome, Firefox, Edge, Opera)
	//And instantiates the specific driver class for that browser
	//Uses the WebDriverManager library that get the version and path of the browser and
	//Download the specific WebDriber binaries for it
	void configureWebDriver()
	{
		int ch=0; //Stores the choice entered by the user
		boolean condition = true; // Variable to prevent infinite while loop
		Scanner sc = new Scanner(System.in);
		System.out.println("Select the browser installed in your machine");
		System.out.println("1. Chrome");
		System.out.println("2. Firefox");
		System.out.println("3. Edge");
		System.out.println("4. Opera\n");
		System.out.println("Enter your choice");
		ch = sc.nextInt();
		
		while(condition)
		{
			switch(ch)
			{
				case 1:
					ChromeOptions chrome= new ChromeOptions();
					WebDriverManager.chromedriver().setup();
					driver = new ChromeDriver(chrome);
					condition = false;
					break;
				case 2:
					FirefoxOptions fire = new FirefoxOptions();
					WebDriverManager.firefoxdriver().setup();
					driver = new FirefoxDriver(fire);
					condition = false;
					break;
				case 3:
					EdgeOptions edge = new EdgeOptions();
					WebDriverManager.edgedriver().setup();
					driver = new EdgeDriver(edge);
					condition = false;
					break;
				case 4:
					OperaOptions opera = new OperaOptions();
					WebDriverManager.operadriver().setup();
					driver = new OperaDriver(opera);
					condition = false;
					break;
				default:
					System.out.println("Enter a valid choice");
					ch = sc.nextInt();
					condition = false;
					break;
			}
			sc.close();
		}
	}
	
	//Instantiates a BufferReader class that read the lines from CompAndRoles.txt
	//And splits them and stores the company and roles in the ArrayList<Struct> list
	void readList()
	{
		String temp = "";
		Struct interim = new Struct();
		String arp[];
		
		File file = new File("CompAndRoles.txt");
		  
		try {
			String st;
			BufferedReader	br = new BufferedReader(new FileReader(file));
			while ((st = br.readLine()) != null) //Reads the file until it reaches the end
			  {  
				  temp = st;
				  arp = temp.split(","); //splits the string with respect to "," yielding an array of strings
				  interim.company = arp[0]; //contains the value of the company
				  interim.role = arp[1]; //contains the values of the role
				  list.add(interim);
				  }
			br.close();
		} catch (FileNotFoundException e1) {
			System.out.println("File not found");
		}
		   catch (IOException e) {
			e.printStackTrace();
		}
		  		
		  
	}
	
	/*
	void login()
	{
		driver.get("https://www.linkedin.com/login");
		driver.findElement(By.id("username")).sendKeys("");
		driver.findElement(By.id("password")).sendKeys("");
		driver.findElement(By.xpath("/html/body/div/main/div[2]/div[1]/form/div[3]/button")).click();
	}
	*/
	
	//Performs a google search that yields the profile links for the given company and role
	void search()
	{
		String baseURL = "site:linkedin.com/in/ AND"; //URL that specifies the site 
		String temp=""; //Temporary string that is used to concatenate the BaseUrl and the company and role 
		driver.get("https://www.google.com"); //WebDriver gets the google search page
		//wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("q"))); 
		WebElement search = driver.findElement(By.name("q")); //Locates the search element by its name
		for(int i=0;i<list.size();i++)
		{
			temp += baseURL + " " + list.get(i).company + " AND " + list.get(i).role; //Adds the baseUrl and company and role to the string temp
			search.sendKeys(temp); //Enters the temp string into the search field
			search.sendKeys(Keys.ENTER); //Presses the enter key to perform the search
			
			//Retrieves the top 10 urls of profiles yielded from the search results
			for(int j=1;j<10;j++)
			{
				WebElement elem = driver.findElement(By.xpath("//*[@id=\"rso\"]/div[" + j + "]/div/div/div[1]/a")); //Gets the <a href> attribute of the profiles displayed from the search result
				urls.add(elem.getAttribute("href")); //href contains the URLS of the linkedin profiles 
			}
			driver.get("https://www.google.com"); //Redirects back to the google search page so that another search can be made
		}
	}
	
	/*
	void scrape()
	{
		for(int i=0;i<urls.size();i++)
		{
			try {
				Document Doc = Jsoup.connect(urls.get(i)).timeout(1000).get();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	*/
	
	
	void connectSql()
	{
		
		 con = null;
		
		try {
			Class.forName("org.sqlite.JDBC");
			con = DriverManager.getConnection("jdbc:sqlite:test.db"); //Connects to the SQL database if it exists if not creates a new one
			Statement stmt = con.createStatement(); //Creates a SQL statement that can be executed
			String sql = "CREATE TABLE IF NOT EXISTS LINKS" + "(URLS TEXT)"; 
			stmt.executeUpdate(sql); //Executes the SQL statement
			stmt.close();
			con.close(); //Closes the SQL connection
			
		} catch(Exception e) {
			System.err.println(e.getClass().getName() + ":" + e.getMessage());
			System.exit(0);
		}
		System.out.println("opened database successfully");
		
	}
	
	//Writes the links to the database
	void writeToDB()
	{
		try {
			Class.forName("org.sqlite.JDBC");
			con = DriverManager.getConnection("jdbc:sqlite:test.db");
			con.setAutoCommit(false);
			Statement stmt = null;
			stmt = con.createStatement();
			for(int i=0;i<urls.size();i++)
			{
				System.out.println(urls.get(i));
				String sql = "INSERT INTO LINKS (URLS)" + "VALUES('" + urls.get(i) + "')"; //inserts the links into the database
				stmt.executeUpdate(sql); 
			}
			stmt.close();
			con.commit();
			con.close();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}
}

public class App 
{		
    public static void main( String[] args )
    {
       Scraper sc = new Scraper();
       sc.run();
    }
}
