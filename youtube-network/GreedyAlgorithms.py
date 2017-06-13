import igraph
import random
from LinearThreshold import compute_k_diffusion

# Compute K influencers seed set using greedy algorithm 
def compute_influencers(g, dg, threshold, K):
    influencers = set()
    for num in range(0, K):
        max_spread_val = 0
        max_spread_node = -1
        temp = list(influencers)
        for v in g.vs:
            if v.index not in temp:
                temp.append(v.index)
                curr_influencers = set(temp)
                curr_spread_val = spread_estimate(dg,  curr_influencers, threshold, K)
                if curr_spread_val > max_spread_val:
                    max_spread_val = curr_spread_val
                    max_spread_node = v.index
                temp.remove(v.index)
        influencers.add(max_spread_node)
    return influencers

# Compute Spread using Linear Threshold
def spread_estimate(dg, influencers, threshold, K):
    curr_active_nodes = compute_k_diffusion(dg, K,  influencers , threshold)
    active_nodes = set(influencers)
    for c in curr_active_nodes:
        active_nodes.add(c)
    num_active_nodes = len(active_nodes)
    return num_active_nodes
     
      
            

