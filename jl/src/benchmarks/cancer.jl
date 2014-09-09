using Sigma

function ground_truth(cancer_rate)
  (cancer_rate * 0.008) / ((0.008 * cancer_rate) + (0.00096 * 0.99))
end

ground_truth(0.001)
breast_cancer = flip(1,0.01)
positive_mammogram = @If breast_cancer flip(2, 0.8) flip(3,0.096)
cond_prob_deep(breast_cancer, positive_mammogram, max_depth = 10)

sigma_stats = plot_cond_performance(breast_cancer, positive_mammogram, num_points = 10)
sigma_layer = stat_errorbar_layer(f,"run_time","probmin","probmax")

church_stats = run_church("rej.church")
church_layer = stat_line_layer(rainbow_church,"run_time","prob")
plot(c,flayer)
