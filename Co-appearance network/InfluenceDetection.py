import igraph
import collections
import random
import time
from collections import OrderedDict
from operator import itemgetter
from LinearThreshold import get_directed_graph, init_edge_influence, init_vertex_threshold, compute_k_diffusion
from GreedyAlgorithms import compute_influencers

# Number of influencers in seed set
k = 7

# Construct graph with vertices and edges
def construct_graph():
    g = igraph.Graph.Read_GML('data/lesmis.gml')  
    return g

# Assign colors to the vertices
def color_vertex(g,influencer_list, spread_list):
    for i in range(0,g.vcount(),1):
        if g.vs[i]["label"] in influencer_list:
            g.vs[i]["color"] = "red"
        elif g.vs[i]["label"] in spread_list:
            g.vs[i]["color"] = "green"
        else:
            g.vs[i]["color"] = "blue"
    return g

# Initialize vertex names
def init_vertex_name(graph):
    for vertex in range(0,graph.vcount(),1):
        graph.vs[vertex]["name"] = graph.vs[vertex]["label"]
    return graph
    
# Assign labels to the vertices
def get_vertex_index_list(graph):
    for vertex in range(0,graph.vcount(),1):
        graph.vs[vertex]["label"] = vertex
    return graph

# Copy vertex labels
def copy_vertex_index_list(graph, spanning_graph):
    for vertex in range(0,graph.vcount(),1):
        spanning_graph.vs[vertex]["label"] = graph.vs[vertex]["label"] 
    return spanning_graph

# Plot graph     
def plot_graph(graph, layoutType, filename):
    layout = graph.layout(layoutType)
    visual_style = {}
    visual_style["vertex_size"] = 12
    visual_style["vertex_label_size"] = 6
    visual_style["layout"] = layout
    visual_style["bbox"] = (450, 450)
    visual_style["margin"] = 10
    igraph.plot(graph, filename, **visual_style)
    
# Compute K influencers seed set
def influencers_by_PageRank(graph, k):
    influencers = []
    graph_centrality = sorted(graph.pagerank(damping=0.85), reverse=True)
    for i in range(0,k):
        influencer_node = graph.vs.select(_pagerank_eq =  graph_centrality[i])
        if influencer_node[0].index not in influencers:
            influencers.append(influencer_node[0].index)
        else :
            for j in range(0, len(influencer_node)):
                if influencer_node[j].index not in influencers:
                    influencers.append(influencer_node[j].index)
                    break
    return influencers

def print_nodes(node_list, g):
    record = ""
    print "Vertices by Id are:"
    print node_list
    print
    print "Vertices by names are:"
    cnt = 0
    for i in node_list:
        if cnt <= 10:
            record =  record + "    " + g.vs[i]["name"] 
            cnt = cnt + 1 
        else:
            record = record + "\n"
            cnt = 0
    print record
    
# Compute the most central vertex by PageRank centrality
def central_node_by_PageRank(graph):
    central_node = graph.vs.select(_pagerank_eq = max(graph.pagerank(damping=0.85)))
    return central_node[0].index

def sort_neighbors(graph, neighbors):
      graph_centrality = graph.pagerank(damping=0.85)
      node_centrality = dict()
      neighbors_set = set()
      for n in neighbors:
          neighbors_set.add(n)
      for n in neighbors_set:
          node_centrality[n] = graph_centrality[n]
      node_centrality_sorted = OrderedDict(sorted(node_centrality.items(), key=itemgetter(1), reverse= True))
      return node_centrality_sorted.keys()
      
# Construct the spanning graph with the most central vertex as starting vertex
def compute_spanning_graph(graph,root):
    visited, queue = set(), collections.deque([root])
    sg_u_list = []
    sg_v_list = []
    while queue:
        vertex = queue.popleft()
        if vertex not in visited:
           visited.add(vertex)
        sorted_neighbors = sort_neighbors(graph, graph.neighbors(vertex))
        for neighbour in sorted_neighbors:
            if neighbour not in visited:
                visited.add(neighbour)
                sg_u_list.append(vertex)
                sg_v_list.append(neighbour)
                queue.append(neighbour)
    sg_edges = zip(sg_u_list, sg_v_list)
    vs = igraph.VertexSeq(graph)
    spanning_graph = igraph.Graph()
    spanning_graph.add_vertices(len(vs))
    spanning_graph.add_edges(sg_edges)
    spanning_graph = copy_vertex_index_list(graph, spanning_graph)
    spanning_graph.vs["color"] = "blue"
    return spanning_graph

def get_influencers(influencers, g, k):
   curr_spanning_influencers = []
   pagerank_central_node = central_node_by_PageRank(g)
   spanning_graph = compute_spanning_graph(g, pagerank_central_node)
   plot_graph(spanning_graph, "lgl", "output/spanning-graph.png")
   spanning_pagerank_central_node = central_node_by_PageRank(spanning_graph)
   print "Most Central vertex in Spanning graph by PageRank is:", spanning_pagerank_central_node
   print
   curr_spanning_influencers = influencers_by_PageRank(spanning_graph , k)
   print "The influncers by PageRank Centrality on Acyclic Spanning Graph are :"
   influencers.append(curr_spanning_influencers)
   print_nodes(influencers[0], g)
   return influencers
    
def compute_spread(dg, seed_set, threshold):
    curr_active_nodes = set()
    influencers_set = set()
    for n in seed_set:
        influencers_set.add(n)
        curr_active_nodes.add(n)
    curr_active_nodes = compute_k_diffusion(dg, k,  influencers_set , threshold)
    return curr_active_nodes

begin_time = time.time()   
g = construct_graph()
print(len(g.vs))
print(len(g.es))
g = init_vertex_name(g)
g = get_vertex_index_list(g)
g.vs["color"] = "blue"
random.seed(123) 
plot_graph(g, "lgl", "output/co-appearance-graph.png")

influencers = []
influencers = get_influencers(influencers, g, k)


print
print "Computing Influence spread of the seed set detected by PageRank centrality using Linear Threshold........"
dg = get_directed_graph(g)
dg = get_vertex_index_list(dg)
dg = init_edge_influence(dg)
threshold = init_vertex_threshold(dg) 

curr_active_nodes = set()
curr_active_nodes = compute_spread(dg, influencers[0], threshold)
print "Total number of Vertices influenced are: ", len(curr_active_nodes)
print_nodes(curr_active_nodes, g)
print

for i in influencers:
    color_vertex(g,i, curr_active_nodes)
random.seed(123)    
plot_graph(g, "lgl", "output/acg-output-graph.png")
print
print("--- Time for Influence detection and spread estimation using PageRank centrality in seconds is  %s   ---" % (time.time() - begin_time))
begin_time = time.time()  
print "Computing Influence seed set with greedy method........"
influencers_greedy = []
influencers_greedy = compute_influencers(g, dg, threshold,k)
print_nodes(influencers_greedy, g)
print
curr_active_nodes = compute_spread(dg, influencers_greedy, threshold)
print "Computing Influence spread of the greedy seed set using Linear Threshold........"
print "Total number of vertices influenced are: ", len(curr_active_nodes)
print_nodes(curr_active_nodes, g)


g.vs["color"] = "blue"
color_vertex(g,influencers_greedy, curr_active_nodes)
random.seed(123)    
plot_graph(g, "lgl", "output/output-greedy-graph.png")

print("--- %s Time for Influence detection and spread estimation using greedy approach in seconds ---" % (time.time() - begin_time))