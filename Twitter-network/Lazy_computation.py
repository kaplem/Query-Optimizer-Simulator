import igraph
import random
import Queue
import time
from LinearThreshold import get_directed_graph, init_edge_influence, init_vertex_threshold, compute_k_diffusion
from vertex_cover import get_vertex_cover
from GreedyAlgorithms import compute_influencers

# Size of seed sets
K = 20                                                                                                                                                                                                                         


# Define Priority Queue structure node
class queue_element():
 def __init__(self, idx, i, j):
  self.index = idx
  self.mg = i
  self.iteration = j

 def __cmp__(self, other):
     return cmp(self.mg, other.mg)

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
 
# Plot graph     
def plot_graph(graph, layoutType, filename):
    layout = graph.layout(layoutType)
    visual_style = {}
    visual_style["vertex_size"] = 18
    visual_style["vertex_label_size"] = 8
    visual_style["layout"] = layout
    visual_style["bbox"] = (450, 400)
    visual_style["margin"] = 20
    igraph.plot(graph, filename, **visual_style)
           
# Construct Social graph
def construct_graph():
    g = igraph.read("data/twitter.edges", format="ncol", directed=True, names = True)
    return g

# Assign vertex labels
def get_vertex_index_list(graph):
    for vertex in range(0,graph.vcount(),1):
        graph.vs[vertex]["label"] = vertex
    return graph
    
              
# Compute Influencer seed set        
def compute_influencers_vertex_cover(g, dg, threshold) :
    influencers = []
    q = Queue.PriorityQueue()
    iteration = 1
    vertex_cover = get_vertex_cover(dg)
    for vertex in vertex_cover:
        temp = [vertex]
        curr_mg = compute_spread(dg, temp,threshold)
        q.put((-curr_mg, queue_element(vertex, curr_mg, iteration)))
    while True:
        if iteration > K:
            break
        max_element = q.get()[1]
        if max_element.iteration == iteration:
            influencers.append(max_element.index)
            iteration = iteration + 1
        else:
             temp = list( influencers )
             temp.append(max_element.index)
             max_element.iteration = iteration
             curr_mg = compute_spread(dg, temp, threshold)
             total_mg = curr_mg - max_element.mg
             q.put((-total_mg,queue_element(max_element.index, total_mg, iteration)))
        
    return influencers

def compute_influencers_celf(g, dg, threshold) :
    influencers = []
    q = Queue.PriorityQueue()
    iteration = 1
    for vertex in g.vs:
        temp = [vertex.index]
        curr_mg = compute_spread(dg, temp,threshold)
        q.put((-curr_mg, queue_element(vertex.index, curr_mg, iteration)))
    while True:
        if iteration > K:
            break
        max_element = q.get()[1]
        if max_element.iteration == iteration:
            influencers.append(max_element.index)
            iteration = iteration + 1
        else:
             temp = []
             temp = list(influencers )
             temp.append(max_element.index)
             max_element.iteration = iteration
             curr_mg = compute_spread(dg, temp, threshold)
             total_mg = curr_mg - max_element.mg
             q.put((-total_mg,queue_element(max_element.index, total_mg, iteration)))
    return influencers
             
# Compute the iformation diffusion spread for current seed set            
def compute_spread(dg, influencers, threshold):
    curr_active_nodes = compute_k_diffusion(dg, K, influencers , threshold)
    active_nodes = set(influencers)
    for c in curr_active_nodes:
        active_nodes.add(c)
    num_active_nodes = len(active_nodes)
    return num_active_nodes
    
def compute_influence_spread(dg, seed_set, threshold):
    curr_active_nodes = set()
    influencers_set = set()
    for n in seed_set:
        influencers_set.add(n)
        curr_active_nodes.add(n)
    curr_active_nodes = compute_k_diffusion(dg, K,  influencers_set , threshold)
    return curr_active_nodes
    
def print_nodes(node_list, g):
    record = ""
    print "Vertices by Id are:"
    print node_list
    print
    print "Vertices by names are:"
    for i in node_list:
        record =  record + " " + g.vs[i]["name"] 
    print record
	
begin_time = time.time()  
g = construct_graph()
g.es["arrow_size"] = 0.45
g = get_vertex_index_list(g)

g.vs["color"] = "blue"
random.seed(123) 
print(len(g.vs))
print(len(g.es))
plot_graph(g, "kk", "output/twitter-graph.png")

dg = get_directed_graph(g)
dg = init_edge_influence(dg)
threshold = init_vertex_threshold(dg)

print "Computing influencers by CELF using Vertex Cover ...."
influencers = []
influencers = compute_influencers_vertex_cover(g, dg, threshold)
print_nodes(influencers, g)
print g.vs[influencers[0]]["name"]
print

print "Computing the spread of seed set ...."
curr_active_nodes = set()
curr_active_nodes = compute_influence_spread(dg, influencers, threshold)
print "Total number of vertices influenced are: ", len(curr_active_nodes)
print_nodes(curr_active_nodes, g)
color_vertex(g, influencers, curr_active_nodes)
random.seed(123)    
plot_graph(g, "kk", "output/output-vertex-graph.png")

print("--- Time for Influence detection and spread estimation using CELF and vertex cover in seconds is  %s   ---" % (time.time() - begin_time))
begin_time = time.time()
print
print "Computing influencers by CELF ...."
influencers_celf = []
influencers_celf = compute_influencers_celf(g, dg, threshold)
print_nodes(influencers_celf, g)

print "Computing the spread of seed set ...."
curr_active_nodes = set()
curr_active_nodes = compute_influence_spread(dg, influencers_celf, threshold)
print "Total number of vertices influenced are: ", len(curr_active_nodes)
print_nodes(curr_active_nodes, g)
color_vertex(g, influencers_celf, curr_active_nodes)
random.seed(123)    
plot_graph(g, "kk", "output/output-celf-graph.png")
print("--- Time for Influence detection and spread estimation using CELF in seconds is  %s   ---" % (time.time() - begin_time))
begin_time = time.time()
print
print "Computing influencers by Greedy ...."
influencers_greedy = []
influencers_greedy = compute_influencers(g, dg, threshold,K)
print_nodes(influencers_greedy, g)
print "Computing the spread of greedy seed set ...."
curr_active_nodes = set()
curr_active_nodes = compute_influence_spread(dg, influencers_greedy, threshold)
print "Total number of vertices influenced are: ", len(curr_active_nodes)
print_nodes(curr_active_nodes, g)
color_vertex(g, influencers_greedy, curr_active_nodes)
random.seed(123)    
plot_graph(g, "kk", "output/output-greedy-graph.png")
print("--- Time for Influence detection and spread estimation using greedy approach in seconds is  %s   ---" % (time.time() - begin_time))
