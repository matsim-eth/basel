package ch.ethz.matsim.basel.osm;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt2matsim.tools.NetworkTools;
import org.matsim.pt2matsim.tools.ScheduleTools;

import java.util.*;

public class CleanNetwork {
	private final static Logger log = Logger.getLogger(CleanNetwork.class);

	protected Network network;
	protected TransitSchedule schedule;
	
	private Set<Node> unusedNodes = new HashSet<Node>();
	private Set<Link> linksWithPTorCS = new HashSet<Link>();
	private Map<Id<Link>, List<Link>> oldLinksNewLinksMap = new HashMap<Id<Link>, List<Link>>();
	
	public CleanNetwork(String networkFile, String scheduleFile){
		this.network = NetworkTools.readNetwork(networkFile);
		this.schedule = ScheduleTools.readTransitSchedule(scheduleFile);
	}
	
	public void run(){
		findLinksWithPTStations();
		findLinksWithCountStations();
		findUnusedNodes();
		for(Node node : this.unusedNodes){
			cleanNetwork(node);
		}
	}
	
	public void findLinksWithPTStations(){
		for(TransitStopFacility PTStation : this.schedule.getFacilities().values()){
			this.linksWithPTorCS.add(this.network.getLinks().get(PTStation.getLinkId()));
		}
	}
	
	public void findLinksWithCountStations(){
		// write method to read links from a csv file and add to linksWithPTorCS.
	}
	
	public void findUnusedNodes(){
		for (Node node : this.network.getNodes().values()){
		    List<Link> inLinks = new ArrayList<Link>();
		    inLinks.addAll(node.getInLinks().values());
		    List<Link> outLinks = new ArrayList<Link>();
		    outLinks.addAll(node.getOutLinks().values());
		    if(Collections.disjoint(inLinks, this.linksWithPTorCS) && Collections.disjoint(outLinks, this.linksWithPTorCS)){
		        if(inLinks.size() == outLinks.size()){
		            if(inLinks.size() == 1){
		                Node fromNode = inLinks.get(0).getFromNode();
		                Node toNode = outLinks.get(0).getToNode();
		                if(!fromNode.equals(toNode)) this.unusedNodes.add(node);
		            }
		            else if(inLinks.size() == 2){
		                Node nextNode1_in = inLinks.get(0).getFromNode();
		                Node nextNode2_in = inLinks.get(1).getFromNode();
		                Node nextNode1_out = outLinks.get(0).getToNode();
		                Node nextNode2_out = outLinks.get(1).getToNode();
		                if(!nextNode1_in.equals(nextNode2_in) && !nextNode1_out.equals(nextNode2_out)){
		                    if(nextNode1_in.equals(nextNode1_out)){
		                        if(nextNode2_in.equals(nextNode2_out)) this.unusedNodes.add(node);
		                    }
		                    else if(nextNode1_in.equals(nextNode2_out)){
		                        if(nextNode2_in.equals(nextNode1_out)) this.unusedNodes.add(node);
		                    }
		                }
		            }
		        }
		    }
		}
	}
	
	public void cleanNetwork(Node nodeToClean){
		Set<Node> extremeNodes = new HashSet<Node>();
		Set<Node> nodesToRemove = new HashSet<Node>();
		Set<Link> linksToRemove = new HashSet<Link>();
		Set<Node> tempNodesToRemove = new HashSet<Node>();
		Set<Link> tempLinksToRemove = new HashSet<Link>();
		nodesToRemove.add(nodeToClean);
		linksToRemove.addAll(nodeToClean.getInLinks().values());
		linksToRemove.addAll(nodeToClean.getOutLinks().values());
		while(extremeNodes.size() < 2){
			for(Node node : nodesToRemove){
				for(Link link : linksToRemove){
					if(this.unusedNodes.contains(link.getFromNode()) && !nodesToRemove.contains(link.getFromNode())){ // found new unused node, add to temporary removal list
						tempNodesToRemove.add(link.getFromNode());
						tempLinksToRemove.addAll(link.getFromNode().getInLinks().values());
						tempLinksToRemove.addAll(link.getFromNode().getOutLinks().values());
					}else if(this.unusedNodes.contains(link.getToNode()) && !nodesToRemove.contains(link.getToNode())){ // found new unused node, add to temporary removal list
						tempNodesToRemove.add(link.getToNode());
						tempLinksToRemove.addAll(link.getToNode().getInLinks().values());
						tempLinksToRemove.addAll(link.getToNode().getOutLinks().values());
					}else if(!this.unusedNodes.contains(link.getFromNode())){
						extremeNodes.add(link.getFromNode());
					}else if(!this.unusedNodes.contains(link.getToNode())){
						extremeNodes.add(link.getToNode());
					}
				}
			}
		nodesToRemove.addAll(tempNodesToRemove);
		linksToRemove.addAll(tempLinksToRemove);
		tempNodesToRemove.clear();
		tempLinksToRemove.clear();
		}
		
		Iterator<Node> iter = extremeNodes.iterator();
		createLink(this.network, iter.next(), iter.next());
		
		for(Link link : linksToRemove) this.network.removeLink(link.getId());
		for(Node node : nodesToRemove) this.network.removeNode(node.getId());
	}

	private void createLink(Network network, Node node1, Node node2){
		Link baseLink1 = null;
		Link baseLink2 = null;
		List<Link> oldLinks1 = new ArrayList<Link>();
		List<Link> oldLinks2 = new ArrayList<Link>();	
		for(Link link : node1.getOutLinks().values()){
			if(this.unusedNodes.contains(link.getToNode())) baseLink1 = link;
		}
		for(Link link : node2.getOutLinks().values()){
			if(this.unusedNodes.contains(link.getToNode())) baseLink2 = link;
		}
		oldLinks1.add(baseLink1);
		oldLinks2.add(baseLink2);
		
		// Move from node1 to node2 and each link in the way to removalList1
		Node checkNode = node1;
		Link nextLink = baseLink1;
		while(!checkNode.equals(node2)){
			for(Link link : nextLink.getToNode().getOutLinks().values()){
				if(!link.getToNode().equals(checkNode)){
					oldLinks1.add(link);
					checkNode = nextLink.getToNode();
					nextLink = link;
				} 
			}
		}
		// Same as above, in the opposite way
		checkNode = node2;
		nextLink = baseLink2;
		while(!checkNode.equals(node1)){
			for(Link link : nextLink.getToNode().getOutLinks().values()){
				if(!link.getToNode().equals(checkNode)){
					oldLinks2.add(link);
					checkNode = nextLink.getToNode();
					nextLink = link;
				} 
			}
		}
		Link newLink1 = network.getFactory().createLink(baseLink1.getId(), node1, node2);
		newLink1.setLength(baseLink1.getLength());
		newLink1.setFreespeed(baseLink1.getFreespeed());
		newLink1.setCapacity(baseLink1.getCapacity());
		newLink1.setNumberOfLanes(baseLink1.getNumberOfLanes());
		newLink1.setAllowedModes(baseLink1.getAllowedModes());
		Link newLink2 = network.getFactory().createLink(baseLink1.getId(), node2, node1);
		newLink2.setLength(baseLink2.getLength());
		newLink2.setFreespeed(baseLink2.getFreespeed());
		newLink2.setCapacity(baseLink2.getCapacity());
		newLink2.setNumberOfLanes(baseLink2.getNumberOfLanes());
		newLink2.setAllowedModes(baseLink2.getAllowedModes());
		
		this.oldLinksNewLinksMap.put(newLink1.getId(), oldLinks1);
		this.oldLinksNewLinksMap.put(newLink1.getId(), oldLinks2);
	}
	
}
