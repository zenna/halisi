using Sigma

groundtruthcancer = ground_truth(0.01)
ground_truth_dist = ptrue_to_dist(ground_truth(0.01))

ptrue_to_dist(ptrue::Float64) = [1 => ptrue, 0 => (1 - ptrue)]
KL(ptrue_to_dist(ground_truth(0.01)),ptrue_to_dist(.9))

function add_KL!(stats, groundtruth::Dict)
  for s in stats
    kl1 = KL(groundtruth, ptrue_to_dist(s["probmin"]))
    kl2 = KL(groundtruth, ptrue_to_dist(s["probmax"]))
    s["klmin"] = min(kl1,kl2)
    s["klmax"] = max(kl1,kl2)
  end
  stats
end

function add_KL_church!(stats, groundtruth::Dict)
  for s in stats
    kl = KL(groundtruth, ptrue_to_dist(s["prob"]))
    s["kl"] = kl
  end
  stats
end

function ground_truth(cancer_rate)
  (cancer_rate * 0.008) / ((0.008 * cancer_rate) + (0.00096 * 0.99))
end

ground_truth(0.01)
breast_cancer = flip(1,0.01)
positive_mammogram = @If breast_cancer flip(2, 0.008) flip(3,0.00096)
cond_prob_deep(breast_cancer, positive_mammogram, max_depth = 12)
cond_prob_deep(breast_cancer, positive_mammogram, max_depth = 8)
sigma_stats = plot_cond_performance(breast_cancer, positive_mammogram, num_points = 10)
add_KL!(sigma_stats, ptrue_to_dist(ground_truth(0.01)))
sigma_layer = stat_errorbar_layer(sigma_stats[2:],"run_time","klmin","klmax")

multi_sigma_stats =
  [plot_cond_performance(breast_cancer, positive_mammogram, num_points = 2, max_depth = i)
   for i = 1:10]

[add_KL!(s,ground_truth_dist) for s in multi_sigma_stats]
multi_sigma_layers = map(s->stat_errorbar_layer(s,"run_time","klmin","klmax"),multi_sigma_stats)

church_stats = run_church("rej.church")
add_KL_church!(church_stats,ground_truth_dist)
church_layer = stat_line_layer(church_stats,"run_time","kl")
plot(multi_sigma_layers[5],church_layer)
