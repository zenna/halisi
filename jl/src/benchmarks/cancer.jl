using Sigma

function ground_truth(cancer_rate)
  (cancer_rate * 0.008) / ((0.008 * cancer_rate) + (0.00096 * 0.99))
end

ground_truth(0.001)
breast_cancer = flip(1,0.001)
positive_mammogram = @If breast_cancer flip(2, 0.008) flip(3,0.00096)
cond_prob_deep(breast_cancer, positive_mammogram, max_depth = 10)
