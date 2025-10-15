local currentWindowKey = KEYS[1]
local previousWindowKey = KEYS[2]
local nowMillis = tonumber(ARGV[1])
local windowDurationMillis = tonumber(ARGV[2])
local subWindowDurationMillis = tonumber(ARGV[3])
local limit = tonumber(ARGV[4])

-- get current window count
local currentCountStr = redis.call('GET', currentWindowKey)
local currentCount = 0
if currentCountStr then
    currentCount = tonumber(currentCountStr)
end

-- get previous window count
local prevCountStr = redis.call('GET', previousWindowKey)
local prevCount = 0
if prevCountStr then
    prevCount = tonumber(prevCountStr)
end

-- calculate weight for previous windows
local elapsedInCurrentWindow = nowMillis % subWindowDurationMillis
local weight = (subWindowDurationMillis - elapsedInCurrentWindow) / subWindowDurationMillis

-- calculate total count by using weight
local total = currentCount + (prevCount * weight)

if total < limit then
    -- Increase current window value and get current
    local count = redis.call('INCR', currentWindowKey)

    -- Set ttl value if first increment operation for current window
    if count == 1 then
        redis.call('PEXPIRE', currentWindowKey, windowDurationMillis)
    end
    return 1
end

return 0