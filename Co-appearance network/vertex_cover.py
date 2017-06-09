import igraph
import random
from collections import OrderedDict
from operator import itemgetter


def get_undirected_graph(g): 
   g_copy = g.copy()
   g_copy.to_undirected()
   return g_copy
     
def construct_graph():
    g = igraph.Graph(directed=True)
    g.add_vertices(12)
    g.add_edges([(0,1),(0,2), (0,3), (1,0) , (1,2), (2,0),(1,4),(4,1) ,(2,1), (2,3),(3,0), (3,2), (3,5), (5,3),(5,1), (1,5),(4,5),(5,4),(4,6),(6,4),(6,7),(7,6),(6,8),(8,6),(6,9),(9,6),(6,10),(10,6),(7,8),(8,7),(8,9),(9,8), (4,7), (7,4), (10, 1), (1, 10), (5,7), (7,5), (7,9), (9,7), (7,8), (8,7)])
    #g = igraph.Graph.Erdos_Renyi(50, m=100, directed=False, loops=True)
    #g = igraph.Graph.Read_GraphML('karate.GraphML')
    #g = igraph.read("twitter.edges", format="ncol", directed=True, names=True)
    return g

def get_vertex_cover(graph):
    vertex_cover = set()
    vertex_degree = dict()
    graph_undirected = get_undirected_graph(graph)
    graph_degree = graph_undirected.vs.degree()
    for num in range(0, len(graph.vs)):
        vertex_degree[num] = graph_degree[num]
    vertex_degree_sorted = OrderedDict(sorted(vertex_degree.items(), key=itemgetter(1), reverse= True))
    vertices =  vertex_degree_sorted.keys()
    for v in vertices:
        neighbors = []
        neighbors = graph.neighbors(v, mode="ALL")
        neighbors_set = set(neighbors)
        if v not in vertex_cover:
            for n in neighbors_set:
                if n not in vertex_cover:
                    vertex_cover.add(v)
                    break
    return vertex_cover

#graph = construct_graph()
#vertex_cover = get_vertex_cover(graph)
#print vertex_cover
