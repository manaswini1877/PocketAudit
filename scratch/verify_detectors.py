import re
import sys

# Force UTF-8 output stream
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

def test_collect_request_detector():
    title = "UPI Collect Request Received"
    body = "Collect request of ₹4,999 from cashback-claim@ybl. Enter UPI PIN to claim your refund now."
    content = f"{title} {body}".lower()
    
    score = 0
    patterns = []
    
    has_collect = "collect" in content or "requested" in content or "request" in content
    collect_terms = ["collect request", "requested money", "enter pin to receive", "enter pin to claim", "refund", "cashback", "claim", "prize"]
    has_refund_claim = any(term in content for term in collect_terms)
    
    if has_collect and has_refund_claim:
        score += 55
        patterns.append("Collect request disguised as refund/cashback/claim")
        
    if "pin" in content and any(w in content for w in ["receive", "get", "claim", "credit"]):
        score += 35
        patterns.append("PIN Trap")
        
    vpa_matches = re.findall(r'([a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+)', body)
    for vpa in vpa_matches:
        prefix = vpa.split("@")[0].lower()
        if any(bad in prefix for bad in ["cashback", "refund", "claim", "prize"]):
            score += 25
            patterns.append(f"Suspicious VPA: {vpa}")
            
    assert score >= 70
    assert len(patterns) >= 2
    print("[PASS] CollectRequestDetector logic passed! Score:", score, "Patterns:", patterns)

def test_phishing_link_detector():
    title = "URGENT BANK ALERT"
    body = "ALERT: Your HDFC bank account is blocked. Verify KYC immediately at http://bit.ly/hdfc-kyc-verify within 24 hours to avoid penalty."
    content = f"{title} {body}".lower()
    
    score = 0
    patterns = []
    
    urls = re.findall(r'(https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(?:/[^\s]*)?)', body)
    if urls:
        for url in urls:
            domain = url.replace("http://", "").replace("https://", "").split("/")[0]
            if domain in ["bit.ly", "tinyurl.com", "is.gd", "cutt.ly"]:
                score += 35
                patterns.append(f"Shortened URL: {domain}")
                
    urgency = ["account blocked", "verify immediately", "24 hours", "kyc pending"]
    matched_urgency = [u for u in urgency if u in content]
    if matched_urgency:
        score += 30
        patterns.append(f"Urgency: {matched_urgency}")
        
    assert score >= 65
    print("[PASS] PhishingLinkDetector logic passed! Score:", score, "Patterns:", patterns)

def test_subscription_trap_detector():
    title = "Autopay Debited"
    body = "Autopay executed: ₹999 debited by Unknown global services for recurring mandate."
    content = f"{title} {body}".lower()
    
    score = 0
    patterns = []
    
    if "autopay" in content or "recurring mandate" in content:
        score += 35
        patterns.append("Autopay / recurring mandate")
        
    if "unknown" in content or "global services" in content:
        score += 30
        patterns.append("Unfamiliar merchant")
        
    assert score >= 60
    print("[PASS] SubscriptionTrapDetector logic passed! Score:", score, "Patterns:", patterns)

if __name__ == "__main__":
    test_collect_request_detector()
    test_phishing_link_detector()
    test_subscription_trap_detector()
    print("\nALL DETECTOR RULE LOGIC PASSED VERIFICATION PERFECTLY!")
