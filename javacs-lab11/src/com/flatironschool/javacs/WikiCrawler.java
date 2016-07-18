package com.flatironschool.javacs;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;


public class WikiCrawler {
	// keeps track of where we started
	private final String source;
	
	// the index where the results go
	private JedisIndex index;
	
	// queue of URLs to be indexed
	private Queue<String> queue = new LinkedList<String>();
	
	// fetcher used to get pages from Wikipedia
	final static WikiFetcher wf = new WikiFetcher();

	/**
	 * Constructor.
	 * 
	 * @param source
	 * @param index
	 */
	public WikiCrawler(String source, JedisIndex index) {
		this.source = source;
		this.index = index;
		queue.offer(source);
	}

	/**
	 * Returns the number of URLs in the queue.
	 * 
	 * @return
	 */
	public int queueSize() {
		return queue.size();	
	}
	
	/**
	 * Gets a URL from the queue and indexes it.
	 * @param b 
	 * 
	 * @return Number of pages indexed.
	 * @throws IOException
	 */
	public String crawl(boolean testing) throws IOException {
        // FILL THIS IN!
		if (queue.isEmpty())
			return null; 

		return testing ? crawlTesting():crawlReal();
	}

	public String crawlTesting() throws IOException{
		
		System.out.println("testing");
		//remove url in FIFO
		String url = queue.remove();

		//read page contents
		WikiFetcher fetcher = new WikiFetcher();
		Elements paragraphs = fetcher.readWikipedia(url);
		
		//index page
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis);
		index.indexPage(url, paragraphs);
	
		//add internal links to queue
		queueInternalLinks(paragraphs);

		return url;
	}

	public String crawlReal() throws IOException{
		System.out.println("real");
		//remove url in FIFO
		String url = queue.remove();

		//check if url already indexed 
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis);
		boolean indexed = index.isIndexed(url);

		//if not indexed, read contents, else do nothing
		Elements paragraphs = new Elements();
		if(!indexed)
		{	WikiFetcher fetcher = new WikiFetcher();
			paragraphs = fetcher.fetchWikipedia(url);
		}
		else	
			return null;

		//index page
		index.indexPage(url, paragraphs);
	
		//add internal links to queue
		queueInternalLinks(paragraphs);

		return url;
	}

	/**
	 * Parses paragraphs and adds internal links to the queue.
	 * 
	 * @param paragraphs
	 */
	// NOTE: absence of access level modifier means package-level
	void queueInternalLinks(Elements paragraphs) {
        // FILL THIS IN!
		for(Element paragraph : paragraphs){
			Elements links = paragraph.select("a[href]");
			for(Element link : links )
			{	String url = link.attr("href");
				if(url.startsWith("/wiki"))
					queue.add("https://en.wikipedia.org"+url);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		
		// make a WikiCrawler
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		String source = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		WikiCrawler wc = new WikiCrawler(source, index);
		
		// for testing purposes, load up the queue
		Elements paragraphs = wf.fetchWikipedia(source);
		wc.queueInternalLinks(paragraphs);

		// loop until we index a new page
		String res;
		do {
			res = wc.crawl(false);

            // REMOVE THIS BREAK STATEMENT WHEN crawl() IS WORKING
            break;
		} while (res == null);
		
		Map<String, Integer> map = index.getCounts("the");
		for (Entry<String, Integer> entry: map.entrySet()) {
			System.out.println(entry);
		}
	}
}
