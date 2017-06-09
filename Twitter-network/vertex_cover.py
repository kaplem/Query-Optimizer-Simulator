from collections import OrderedDict
from operator import itemgetter


def get_undirected_graph(g): 
   g_copy = g.copy()
   g_copy.to_undirected()
   return g_copy

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


