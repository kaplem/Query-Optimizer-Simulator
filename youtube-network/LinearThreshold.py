import igraph
from igraph import *
from random import uniform

# Convert undirected to directed graph
def get_directed_graph(g):    
     if not g.is_directed():
         g_copy = g.copy()
         g_copy.to_directed(mutual=True)
         edges = set(g_copy.get_edgelist())
         dg = igraph.Graph(directed=True)
         dg.add_vertices(len(g_copy.vs))
         dg.add_edges(edges)
         return dg
     else:
        return g
        
# Assign Weights to edges      
def init_edge_influence(dg):
     degrees = dg.degree(mode="in")
     edge_influence = []
     for e in dg.es:
        if degrees[e.target] == 0:
             edge_influence.append(0)
        else :
            d = float(degrees[e.target])
            edge_influence.append(1/d)
     #edge_influence_norm = [float(i)/sum(edge_influence) for i in edge_influence]
     dg.es["influence"]=edge_influence
     return dg
 
    
def init_vertex_threshold(dg):
    threshold = dict()
    degree_val = dg.degree()
    for n in range (0, len(degree_val)):
        val =  (degree_val[n] * 10)/(sum(degree_val) * 1.0)
        if val < 0.1:
            val =  val * 10
        threshold[n] = val
        if threshold[n] > 1.0:
            threshold[n] = 1.0
    return threshold

# Calculate the sum of incoming active neighbors   
def sum_edge_influence(dg, active_neighbor, n):
    sum = 0.0
    for src in active_neighbor:
        edge = set(dg.es.select(_source_eq =src, _target_eq =n))
        for e in edge:
            sum = sum + dg.es[dg.get_eid(e.source,e.target)]["influence"]
    if sum > 1:
        sum = 1.0
    return sum  

# Compute information diffusionS 
def compute_k_diffusion(dg, k, seed_set, threshold):
    curr_active_nodes = set(seed_set)
    total_influenced = seed_set
    total_k_influenced_nodes = []
    while(True):
        total_k_influenced_nodes = compute_curr_diffusion(dg, curr_active_nodes, threshold)
        if len(total_k_influenced_nodes) == len(total_influenced):
            break
        curr_active_nodes = list( total_k_influenced_nodes)
        total_influenced = list(total_k_influenced_nodes)
    return curr_active_nodes

# Compute diffusion in current round    
def compute_curr_diffusion(dg, seed_set, threshold):
    curr_seed_set = set(seed_set)
    curr_seed_list = list(seed_set)
    for num in range(0, len(curr_seed_list)):
        neighbors = dg.successors(curr_seed_list[num])
        for n in neighbors:
            if (n not in curr_seed_list):
                active_neighbor = list(set(dg.predecessors(n)).intersection(set(curr_seed_list)))
                sum = sum_edge_influence(dg, active_neighbor, n)
                if (sum - float(threshold[n])) > 0: 
                     curr_seed_set.add(n)
                curr_seed_list = list(curr_seed_set)
    return curr_seed_list
        
